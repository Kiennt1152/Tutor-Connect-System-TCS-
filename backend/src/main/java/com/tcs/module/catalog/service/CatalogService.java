package com.tcs.module.catalog.service;

import com.tcs.module.catalog.dto.response.CatalogItemResponse;
import com.tcs.module.catalog.dto.response.FaqResponse;
import com.tcs.module.catalog.dto.response.LocationResponse;
import java.util.List;

public interface CatalogService {

    List<CatalogItemResponse> getSubjects();

    List<CatalogItemResponse> getCategories();

    List<CatalogItemResponse> getGrades();

    List<CatalogItemResponse> getProvinces();

    List<CatalogItemResponse> getDistricts(Long provinceId);

    List<CatalogItemResponse> getWards(Long districtId);

    List<LocationResponse> getLocations(Long provinceId);

    List<FaqResponse> getFaqEntries();
}
