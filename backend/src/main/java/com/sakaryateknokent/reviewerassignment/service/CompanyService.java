package com.sakaryateknokent.reviewerassignment.service;

import com.sakaryateknokent.reviewerassignment.dto.CompanyContactResponse;
import com.sakaryateknokent.reviewerassignment.dto.CompanyListResponse;
import com.sakaryateknokent.reviewerassignment.dto.CompanyResponse;
import com.sakaryateknokent.reviewerassignment.dto.CreateCompanyContactRequest;
import com.sakaryateknokent.reviewerassignment.dto.CreateCompanyRequest;
import com.sakaryateknokent.reviewerassignment.dto.PageResponse;
import com.sakaryateknokent.reviewerassignment.dto.UpdateCompanyContactRequest;
import com.sakaryateknokent.reviewerassignment.dto.UpdateCompanyRequest;
import com.sakaryateknokent.reviewerassignment.entity.Company;
import com.sakaryateknokent.reviewerassignment.entity.CompanyContact;
import com.sakaryateknokent.reviewerassignment.exception.DuplicateResourceException;
import com.sakaryateknokent.reviewerassignment.exception.InvalidRequestException;
import com.sakaryateknokent.reviewerassignment.exception.ResourceNotFoundException;
import com.sakaryateknokent.reviewerassignment.repository.CompanyContactRepository;
import com.sakaryateknokent.reviewerassignment.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CompanyService {

    private static final Sort DEFAULT_COMPANY_SORT =
            Sort.by(Sort.Order.asc("name").ignoreCase());

    private static final Set<Integer> ALLOWED_PAGE_SIZES =
            Set.of(10, 15, 20, 25);

    private final CompanyRepository companyRepository;
    private final CompanyContactRepository companyContactRepository;

    public CompanyResponse createCompany(CreateCompanyRequest request) {
        String normalizedEmail = normalizeEmail(request.email());

        validateEmailForCreate(normalizedEmail);

        List<NormalizedContactRequest> contacts =
                normalizeCreateContacts(request.contacts());

        Company company = new Company();
        company.setName(request.name().trim());
        company.setEmail(normalizedEmail);
        company.setPhone(normalizeOptionalText(request.phone()));
        company.setActivityArea(normalizeOptionalText(request.activityArea()));
        company.setLogoUrl(normalizeOptionalText(request.logoUrl()));

        for (NormalizedContactRequest contact : contacts) {
            CompanyContact companyContact = new CompanyContact();
            applyContact(companyContact, contact, contact.primary());
            company.addContact(companyContact);
        }

        return toResponse(companyRepository.saveAndFlush(company));
    }

    @Transactional(readOnly = true)
    public PageResponse<CompanyListResponse> getCompanies(
            String search,
            int page,
            int size
    ) {
        validatePagination(page, size);

        String normalizedSearch = search == null
                ? ""
                : search.trim();

        Pageable pageable = PageRequest.of(
                page,
                size,
                DEFAULT_COMPANY_SORT
        );

        Page<Company> companyPage = companyRepository
                .search(normalizedSearch, pageable);

        List<Company> companies = companyPage.getContent();
        Map<Long, List<CompanyContact>> contactsByCompanyId =
                findContactsByCompanyId(companies);

        List<CompanyListResponse> responses = companies.stream()
                .map(company -> toListResponse(
                        company,
                        contactsByCompanyId.getOrDefault(
                                company.getId(),
                                List.of()
                        )
                ))
                .toList();

        return new PageResponse<>(
                responses,
                companyPage.getNumber(),
                companyPage.getSize(),
                companyPage.getTotalElements(),
                companyPage.getTotalPages(),
                companyPage.isFirst(),
                companyPage.isLast()
        );
    }

    @Transactional(readOnly = true)
    public CompanyResponse getCompanyById(Long id) {
        return toResponse(findCompanyById(id));
    }

    public CompanyResponse updateCompany(
            Long id,
            UpdateCompanyRequest request
    ) {
        Company company = findCompanyById(id);
        String normalizedEmail = normalizeEmail(request.email());

        validateEmailForUpdate(normalizedEmail, id);

        List<NormalizedContactRequest> contacts =
                normalizeUpdateContacts(request.contacts());

        company.setName(request.name().trim());
        company.setEmail(normalizedEmail);
        company.setPhone(normalizeOptionalText(request.phone()));
        company.setActivityArea(normalizeOptionalText(request.activityArea()));
        company.setLogoUrl(normalizeOptionalText(request.logoUrl()));

        applyUpdatedContacts(company, contacts);

        return toResponse(company);
    }

    public void deleteCompany(Long id) {
        Company company = findCompanyById(id);
        companyRepository.delete(company);
    }

    private Company findCompanyById(Long id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Şirket bulunamadı: " + id
                ));
    }

    private void validateEmailForCreate(String email) {
        if (email != null && companyRepository.existsByEmail(email)) {
            throw new DuplicateResourceException(
                    "Bu e-posta adresiyle kayıtlı bir şirket zaten var"
            );
        }
    }

    private void validateEmailForUpdate(String email, Long id) {
        if (email != null
                && companyRepository.existsByEmailAndIdNot(email, id)) {
            throw new DuplicateResourceException(
                    "Bu e-posta adresiyle kayıtlı başka bir şirket zaten var"
            );
        }
    }

    private String normalizeEmail(String email) {
        String normalizedEmail = normalizeOptionalText(email);

        return normalizedEmail == null
                ? null
                : normalizedEmail.toLowerCase(Locale.ROOT);
    }

    private String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    private List<NormalizedContactRequest> normalizeCreateContacts(
            List<CreateCompanyContactRequest> contacts
    ) {
        validateContactListExists(contacts);

        List<NormalizedContactRequest> normalizedContacts =
                new ArrayList<>();

        for (CreateCompanyContactRequest contact : contacts) {
            if (contact == null) {
                throw new InvalidRequestException(
                        "Muhatap bilgisi boş olamaz"
                );
            }

            normalizedContacts.add(new NormalizedContactRequest(
                    null,
                    normalizeRequiredContactName(contact.fullName()),
                    normalizeOptionalText(contact.position()),
                    normalizeEmail(contact.email()),
                    normalizeOptionalText(contact.phone()),
                    contact.primary()
            ));
        }

        validatePrimaryContactCount(normalizedContacts);
        validateContactMethods(normalizedContacts);

        return normalizedContacts;
    }

    private List<NormalizedContactRequest> normalizeUpdateContacts(
            List<UpdateCompanyContactRequest> contacts
    ) {
        validateContactListExists(contacts);

        Set<Long> seenIds = new HashSet<>();
        List<NormalizedContactRequest> normalizedContacts =
                new ArrayList<>();

        for (UpdateCompanyContactRequest contact : contacts) {
            if (contact == null) {
                throw new InvalidRequestException(
                        "Muhatap bilgisi boş olamaz"
                );
            }

            Long contactId = contact.id();

            if (contactId != null && !seenIds.add(contactId)) {
                throw new InvalidRequestException(
                        "Aynı muhatap kişi birden fazla kez gönderilemez"
                );
            }

            normalizedContacts.add(new NormalizedContactRequest(
                    contactId,
                    normalizeRequiredContactName(contact.fullName()),
                    normalizeOptionalText(contact.position()),
                    normalizeEmail(contact.email()),
                    normalizeOptionalText(contact.phone()),
                    contact.primary()
            ));
        }

        validatePrimaryContactCount(normalizedContacts);
        validateContactMethods(normalizedContacts);

        return normalizedContacts;
    }

    private void validateContactListExists(List<?> contacts) {
        if (contacts == null || contacts.isEmpty()) {
            throw new InvalidRequestException(
                    "En az bir muhatap kişi eklenmelidir"
            );
        }
    }

    private String normalizeRequiredContactName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            throw new InvalidRequestException(
                    "Muhatap adı soyadı boş bırakılamaz"
            );
        }

        return fullName.trim();
    }

    private void validatePrimaryContactCount(
            List<NormalizedContactRequest> contacts
    ) {
        long primaryCount = contacts.stream()
                .filter(NormalizedContactRequest::primary)
                .count();

        if (primaryCount != 1) {
            throw new InvalidRequestException(
                    "Tam olarak bir muhatap kişi birincil olarak seçilmelidir"
            );
        }
    }

    private void validateContactMethods(
            List<NormalizedContactRequest> contacts
    ) {
        for (NormalizedContactRequest contact : contacts) {
            if (contact.email() == null && contact.phone() == null) {
                throw new InvalidRequestException(
                        "Muhatap kişi için e-posta veya telefon bilgilerinden en az biri girilmelidir"
                );
            }
        }
    }

    private void applyUpdatedContacts(
            Company company,
            List<NormalizedContactRequest> requests
    ) {
        Map<Long, CompanyContact> contactsById = company.getContacts()
                .stream()
                .filter(contact -> contact.getId() != null)
                .collect(Collectors.toMap(
                        CompanyContact::getId,
                        Function.identity()
                ));

        Set<Long> requestedIds = requests.stream()
                .map(NormalizedContactRequest::id)
                .filter(contactId -> contactId != null)
                .collect(Collectors.toSet());

        for (Long contactId : requestedIds) {
            if (!contactsById.containsKey(contactId)) {
                throw new InvalidRequestException(
                        "Muhatap kişi bulunamadı veya bu şirkete ait değil: "
                                + contactId
                );
            }
        }

        List<CompanyContact> contactsToRemove = company.getContacts()
                .stream()
                .filter(contact -> contact.getId() != null
                        && !requestedIds.contains(contact.getId()))
                .toList();

        contactsToRemove.forEach(company::removeContact);

        Map<NormalizedContactRequest, CompanyContact> contactByRequest =
                new HashMap<>();

        for (NormalizedContactRequest request : requests) {
            CompanyContact contact = request.id() == null
                    ? new CompanyContact()
                    : contactsById.get(request.id());

            applyContact(contact, request, false);

            if (request.id() == null) {
                company.addContact(contact);
            }

            contactByRequest.put(request, contact);
        }

        companyRepository.flush();

        requests.stream()
                .filter(NormalizedContactRequest::primary)
                .findFirst()
                .map(contactByRequest::get)
                .ifPresent(contact -> contact.setPrimary(true));
    }

    private void applyContact(
            CompanyContact contact,
            NormalizedContactRequest request,
            boolean primary
    ) {
        contact.setFullName(request.fullName());
        contact.setPosition(request.position());
        contact.setEmail(request.email());
        contact.setPhone(request.phone());
        contact.setPrimary(primary);
    }

    private Map<Long, List<CompanyContact>> findContactsByCompanyId(
            List<Company> companies
    ) {
        if (companies.isEmpty()) {
            return Map.of();
        }

        List<Long> companyIds = companies.stream()
                .map(Company::getId)
                .toList();

        return companyContactRepository.findByCompanyIdIn(companyIds)
                .stream()
                .collect(Collectors.groupingBy(
                        contact -> contact.getCompany().getId()
                ));
    }

    private void validatePagination(int page, int size) {
        if (page < 0) {
            throw new InvalidRequestException(
                    "Sayfa numarası 0 veya daha büyük olmalıdır"
            );
        }

        if (!ALLOWED_PAGE_SIZES.contains(size)) {
            throw new InvalidRequestException(
                    "Sayfa boyutu yalnızca 10, 15, 20 veya 25 olabilir"
            );
        }
    }

    private CompanyListResponse toListResponse(
            Company company,
            List<CompanyContact> contacts
    ) {
        List<CompanyContact> sortedContacts = sortContacts(contacts);

        CompanyContactResponse primaryContact = sortedContacts.stream()
                .filter(CompanyContact::isPrimary)
                .findFirst()
                .map(this::toContactResponse)
                .orElse(null);

        return new CompanyListResponse(
                company.getId(),
                company.getName(),
                company.getEmail(),
                company.getPhone(),
                company.getActivityArea(),
                company.getLogoUrl(),
                primaryContact,
                sortedContacts.size()
        );
    }

    private CompanyResponse toResponse(Company company) {
        return new CompanyResponse(
                company.getId(),
                company.getName(),
                company.getEmail(),
                company.getPhone(),
                company.getActivityArea(),
                company.getLogoUrl(),
                sortContacts(company.getContacts()).stream()
                        .map(this::toContactResponse)
                        .toList()
        );
    }

    private CompanyContactResponse toContactResponse(CompanyContact contact) {
        return new CompanyContactResponse(
                contact.getId(),
                contact.getFullName(),
                contact.getPosition(),
                contact.getEmail(),
                contact.getPhone(),
                contact.isPrimary()
        );
    }

    private List<CompanyContact> sortContacts(List<CompanyContact> contacts) {
        return contacts.stream()
                .sorted(Comparator
                        .comparing(
                                CompanyContact::isPrimary,
                                Comparator.reverseOrder()
                        )
                        .thenComparing(
                                CompanyContact::getId,
                                Comparator.nullsLast(Long::compareTo)
                        ))
                .toList();
    }

    private record NormalizedContactRequest(
            Long id,
            String fullName,
            String position,
            String email,
            String phone,
            boolean primary
    ) {
    }
}
