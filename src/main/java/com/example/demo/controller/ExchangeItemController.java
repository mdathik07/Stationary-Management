package com.example.demo.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.entity.ExchangeItem;
import com.example.demo.entity.StoredFile;
import com.example.demo.entity.User;
import com.example.demo.repository.ExchangeItemRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.FileStorageService;
import com.example.demo.service.impl.CustomUserDetails;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/student")
public class ExchangeItemController {

    private final ExchangeItemRepository exchangeItemRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    @GetMapping("/items")
    public String viewItems(@RequestParam(value = "q", required = false) String q,
                            @RequestParam(value = "category", required = false) String category,
                            Model model) {
        String query = (q == null || q.isBlank()) ? null : q.trim();
        String cat = (category == null || category.isBlank()) ? null : category.trim();
        model.addAttribute("items", exchangeItemRepository.search(query, cat));
        model.addAttribute("categories", exchangeItemRepository.findActiveCategories());
        model.addAttribute("q", q);
        model.addAttribute("selectedCategory", category);
        return "browse_items";
    }

    @GetMapping("/post-item")
    public String showPostItemForm(Model model) {
        model.addAttribute("item", new ExchangeItem());
        return "post_item";
    }

    @PostMapping("/post-item")
    public String handleItemPost(@RequestParam("itemName") String itemName,
                                 @RequestParam(value = "description", required = false) String description,
                                 @RequestParam(value = "category", required = false) String category,
                                 @RequestParam(value = "price", required = false) BigDecimal price,
                                 @RequestParam(value = "photo", required = false) MultipartFile photo,
                                 @AuthenticationPrincipal CustomUserDetails userDetails,
                                 RedirectAttributes redirectAttributes) {
        if (itemName == null || itemName.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Item name is required.");
            return "redirect:/student/post-item";
        }
        if (price != null && price.signum() < 0) {
            redirectAttributes.addFlashAttribute("error", "Price cannot be negative.");
            return "redirect:/student/post-item";
        }

        StoredFile image = null;
        if (photo != null && !photo.isEmpty()) {
            try {
                image = fileStorageService.storeImage(photo);
            } catch (IllegalArgumentException e) {
                redirectAttributes.addFlashAttribute("error", e.getMessage());
                return "redirect:/student/post-item";
            }
        }

        ExchangeItem item = ExchangeItem.builder()
                .itemName(itemName.trim())
                .description(description == null ? null : description.trim())
                .category(category == null || category.isBlank() ? null : category.trim())
                .price(price)
                .image(image)
                .available(true)
                .createdAt(LocalDateTime.now())
                .seller(currentUser(userDetails))
                .build();
        exchangeItemRepository.save(item);

        redirectAttributes.addFlashAttribute("success", "Item posted!");
        return "redirect:/student/my-items";
    }

    @GetMapping("/my-items")
    public String myItems(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        model.addAttribute("items",
                exchangeItemRepository.findBySellerOrderByCreatedAtDesc(currentUser(userDetails)));
        return "my_items";
    }

    @PostMapping("/items/{id}/mark-sold")
    public String markItemAsSold(@PathVariable Long id,
                                 @AuthenticationPrincipal CustomUserDetails userDetails,
                                 RedirectAttributes redirectAttributes) {
        ExchangeItem item = ownItemOrNull(id, userDetails);
        if (item == null) {
            redirectAttributes.addFlashAttribute("error", "You can only manage your own items.");
        } else {
            item.setAvailable(false);
            exchangeItemRepository.save(item);
            redirectAttributes.addFlashAttribute("success", "Marked as sold.");
        }
        return "redirect:/student/my-items";
    }

    @PostMapping("/items/{id}/delete")
    public String deleteItem(@PathVariable Long id,
                             @AuthenticationPrincipal CustomUserDetails userDetails,
                             RedirectAttributes redirectAttributes) {
        ExchangeItem item = ownItemOrNull(id, userDetails);
        if (item == null) {
            redirectAttributes.addFlashAttribute("error", "You can only manage your own items.");
        } else {
            exchangeItemRepository.delete(item);
            redirectAttributes.addFlashAttribute("success", "Listing removed.");
        }
        return "redirect:/student/my-items";
    }

    @GetMapping("/interest/{id}")
    public String expressInterest(@PathVariable Long id,
                                  @AuthenticationPrincipal CustomUserDetails userDetails,
                                  Model model) {
        ExchangeItem item = exchangeItemRepository.findById(id).orElse(null);
        if (item == null || !item.isAvailable()) {
            return "redirect:/student/items";
        }
        boolean isOwner = item.getSeller() != null
                && item.getSeller().getId().equals(userDetails.getUser().getId());
        if (!isOwner) {
            item.setInterestCount(item.getInterestCount() + 1);
            exchangeItemRepository.save(item);
        }
        model.addAttribute("item", item);
        model.addAttribute("seller", item.getSeller());
        return "interest_contact";
    }

    @GetMapping("/items/{id}/image")
    public ResponseEntity<byte[]> itemImage(@PathVariable Long id) {
        return exchangeItemRepository.findById(id)
                .map(ExchangeItem::getImage)
                .map(img -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(img.getContentType()))
                        .header(HttpHeaders.CACHE_CONTROL, "max-age=3600")
                        .body(img.getData()))
                .orElse(ResponseEntity.notFound().build());
    }

    private ExchangeItem ownItemOrNull(Long id, CustomUserDetails userDetails) {
        ExchangeItem item = exchangeItemRepository.findById(id).orElse(null);
        if (item == null || item.getSeller() == null
                || !item.getSeller().getId().equals(userDetails.getUser().getId())) {
            return null;
        }
        return item;
    }

    private User currentUser(CustomUserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalStateException("Logged-in user not found in database"));
    }
}
