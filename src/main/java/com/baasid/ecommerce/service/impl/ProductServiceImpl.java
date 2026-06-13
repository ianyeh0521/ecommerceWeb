package com.baasid.ecommerce.service.impl;

import com.baasid.ecommerce.dto.request.ProductRequest;
import com.baasid.ecommerce.dto.response.ProductResponse;
import com.baasid.ecommerce.entity.Product;
import com.baasid.ecommerce.exception.NotFoundException;
import com.baasid.ecommerce.repository.ProductRepository;
import com.baasid.ecommerce.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Override
    public ProductResponse createProduct(ProductRequest request) {
        Product product = new Product();
        product.setName(request.getName());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setIsPublished(false);
        product.setIsDeleted(false);
        return ProductResponse.from(productRepository.save(product));
    }

    @Override
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .filter(p -> !p.getIsDeleted())
                .orElseThrow(() -> new NotFoundException("找不到商品"));

        if (request.getName() != null) {
            product.setName(request.getName());
        }
        if (request.getPrice() != null) {
            product.setPrice(request.getPrice());
        }
        if (request.getStock() != null) {
            product.setStock(request.getStock());
        }
        if (request.getIsPublished() != null) {
            product.setIsPublished(request.getIsPublished());
        }

        return ProductResponse.from(productRepository.save(product));
    }

    @Override
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .filter(p -> !p.getIsDeleted())
                .orElseThrow(() -> new NotFoundException("找不到商品"));

        product.setIsDeleted(true);
        productRepository.save(product);
    }

    @Override
    public Page<ProductResponse> getProducts(Pageable pageable, boolean isAdmin) {
        if (isAdmin) {
            return productRepository.findAllByIsDeletedFalse(pageable).map(ProductResponse::from);
        }
        return productRepository.findAllByIsDeletedFalseAndIsPublishedTrue(pageable).map(ProductResponse::from);
    }

    @Override
    public ProductResponse getProduct(Long id, boolean isAdmin) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("找不到商品"));

        if (product.getIsDeleted()) {
            throw new NotFoundException("找不到商品");
        }
        if (!isAdmin && !product.getIsPublished()) {
            throw new NotFoundException("找不到商品");
        }

        return ProductResponse.from(product);
    }
}
