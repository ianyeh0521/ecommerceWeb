package com.baasid.ecommerce.service;

import com.baasid.ecommerce.dto.request.ProductRequest;
import com.baasid.ecommerce.dto.response.ProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductService {

    ProductResponse createProduct(ProductRequest request);

    ProductResponse updateProduct(Long id, ProductRequest request);

    void deleteProduct(Long id);

    Page<ProductResponse> getProducts(Pageable pageable, boolean isAdmin);

    ProductResponse getProduct(Long id, boolean isAdmin);
}
