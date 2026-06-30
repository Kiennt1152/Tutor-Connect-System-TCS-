package com.tcs.module.catalog.service.impl;

import com.tcs.module.catalog.dto.response.CatalogItemResponse;
import com.tcs.module.catalog.dto.response.FaqResponse;
import com.tcs.module.catalog.dto.response.LocationResponse;
import com.tcs.module.catalog.entity.Category;
import com.tcs.module.catalog.entity.FaqEntry;
import com.tcs.module.catalog.entity.Grade;
import com.tcs.module.catalog.entity.Location;
import com.tcs.module.catalog.entity.Province;
import com.tcs.module.catalog.entity.Subject;
import com.tcs.module.catalog.repository.CategoryRepository;
import com.tcs.module.catalog.repository.FaqEntryRepository;
import com.tcs.module.catalog.repository.GradeRepository;
import com.tcs.module.catalog.repository.LocationRepository;
import com.tcs.module.catalog.repository.ProvinceRepository;
import com.tcs.module.catalog.repository.SubjectRepository;
import com.tcs.module.catalog.service.CatalogService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CatalogServiceImpl implements CatalogService {

    private final SubjectRepository subjectRepository;
    private final CategoryRepository categoryRepository;
    private final GradeRepository gradeRepository;
    private final ProvinceRepository provinceRepository;
    private final LocationRepository locationRepository;
    private final FaqEntryRepository faqEntryRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CatalogItemResponse> getSubjects() {
        return subjectRepository.findAll().stream().map(this::toItem).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CatalogItemResponse> getCategories() {
        return categoryRepository.findAll().stream()
                .map(c -> CatalogItemResponse.builder()
                        .id(c.getCategoryId())
                        .name(c.getName())
                        .description(c.getDescription())
                        .build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CatalogItemResponse> getGrades() {
        return gradeRepository.findAll().stream()
                .map(g -> CatalogItemResponse.builder()
                        .id(g.getGradeId())
                        .name(g.getGradeName())
                        .build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CatalogItemResponse> getProvinces() {
        return provinceRepository.findAll().stream()
                .map(p -> CatalogItemResponse.builder()
                        .id(p.getProvinceId())
                        .name(p.getProvinceName())
                        .build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LocationResponse> getLocations(Long provinceId) {
        List<Location> locations =
                provinceId != null ? locationRepository.findByProvince_ProvinceId(provinceId) : locationRepository.findAll();
        return locations.stream().map(this::toLocation).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FaqResponse> getFaqEntries() {
        return faqEntryRepository.findAll().stream().map(this::toFaq).toList();
    }

    private CatalogItemResponse toItem(Subject subject) {
        return CatalogItemResponse.builder()
                .id(subject.getSubjectId())
                .name(subject.getSubjectName())
                .description(subject.getDescription())
                .build();
    }

    private LocationResponse toLocation(Location location) {
        Province province = location.getProvince();
        return LocationResponse.builder()
                .locationId(location.getLocationId())
                .provinceId(province != null ? province.getProvinceId() : null)
                .provinceName(province != null ? province.getProvinceName() : null)
                .districtName(location.getDistrictName())
                .wardName(location.getWardName())
                .build();
    }

    private FaqResponse toFaq(FaqEntry entry) {
        return FaqResponse.builder()
                .faqId(entry.getFaqId())
                .question(entry.getQuestion())
                .answer(entry.getAnswer())
                .category(entry.getCategory())
                .build();
    }
}
