package com.sakaryateknokent.reviewerassignment.service;

import com.sakaryateknokent.reviewerassignment.dto.CompanyContactMethodRequest;
import com.sakaryateknokent.reviewerassignment.dto.CompanyContactMethodResponse;
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
import com.sakaryateknokent.reviewerassignment.entity.CompanyContactMethod;
import com.sakaryateknokent.reviewerassignment.enums.CompanyContactMethodType;
import com.sakaryateknokent.reviewerassignment.enums.CompanyType;
import com.sakaryateknokent.reviewerassignment.exception.DuplicateResourceException;
import com.sakaryateknokent.reviewerassignment.exception.InvalidRequestException;
import com.sakaryateknokent.reviewerassignment.exception.ResourceNotFoundException;
import com.sakaryateknokent.reviewerassignment.repository.CompanyContactMethodRepository;
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
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CompanyService {

    private static final Sort DEFAULT_COMPANY_SORT =
            Sort.by(Sort.Order.asc("name").ignoreCase());

    private static final Set<Integer> ALLOWED_PAGE_SIZES =
            Set.of(10, 15, 20, 25);

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"
    );

    private final CompanyRepository companyRepository;
    private final CompanyContactRepository companyContactRepository;
    private final CompanyContactMethodRepository companyContactMethodRepository;

    public CompanyResponse createCompany(CreateCompanyRequest request) {
        List<NormalizedContactMethodRequest> contactMethods =
                normalizeCreateContactMethods(request.contactMethods());
        validateEmailAvailabilityForCreate(contactMethods);

        List<NormalizedContactRequest> contacts =
                normalizeCreateContacts(request.contacts());

        Company company = new Company();
        company.setName(request.name().trim());
        company.setActivityArea(normalizeOptionalText(request.activityArea()));
        company.setLogoUrl(normalizeOptionalText(request.logoUrl()));
        company.setCompanyType(request.companyType());

        for (NormalizedContactMethodRequest contactMethod : contactMethods) {
            CompanyContactMethod companyContactMethod =
                    new CompanyContactMethod();
            applyContactMethod(companyContactMethod, contactMethod);
            company.addContactMethod(companyContactMethod);
        }

        for (NormalizedContactRequest contact : contacts) {
            CompanyContact companyContact = new CompanyContact();
            applyContact(companyContact, contact, contact.primary());
            company.addContact(companyContact);
        }

        return toResponse(companyRepository.saveAndFlush(company));
    }

    @Transactional(readOnly = true)
    public PageResponse<CompanyListResponse> getCompanies(
            CompanyType companyType,
            String search,
            String initial,
            int page,
            int size
    ) {
        validatePagination(page, size);

        String normalizedSearch = search == null
                ? ""
                : search.trim();
        String normalizedInitial = initial == null
                ? ""
                : initial.trim();

        Pageable pageable = PageRequest.of(
                page,
                size,
                DEFAULT_COMPANY_SORT
        );

        Page<Company> companyPage = companyRepository
                .search(companyType, normalizedSearch, normalizedInitial, pageable);

        List<Company> companies = companyPage.getContent();
        Map<Long, List<CompanyContact>> contactsByCompanyId =
                findContactsByCompanyId(companies);
        Map<Long, List<CompanyContactMethod>> contactMethodsByCompanyId =
                findContactMethodsByCompanyId(companies);

        List<CompanyListResponse> responses = companies.stream()
                .map(company -> toListResponse(
                        company,
                        contactsByCompanyId.getOrDefault(
                                company.getId(),
                                List.of()
                        ),
                        contactMethodsByCompanyId.getOrDefault(
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
    public long countCompanies(CompanyType companyType) {
        return companyRepository.countByCompanyType(companyType);
    }

    @Transactional(readOnly = true)
    public List<Long> getCompanyIds(
            CompanyType companyType,
            String search,
            String initial
    ) {
        String normalizedSearch = search == null
                ? ""
                : search.trim();
        String normalizedInitial = initial == null
                ? ""
                : initial.trim();

        return companyRepository
                .search(
                        companyType,
                        normalizedSearch,
                        normalizedInitial,
                        Pageable.unpaged(DEFAULT_COMPANY_SORT)
                )
                .stream()
                .map(Company::getId)
                .toList();
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
        List<NormalizedContactMethodRequest> contactMethods =
                normalizeUpdateContactMethods(request.contactMethods());
        validateEmailAvailabilityForUpdate(contactMethods, id);

        List<NormalizedContactRequest> contacts =
                normalizeUpdateContacts(request.contacts());

        company.setName(request.name().trim());
        company.setActivityArea(normalizeOptionalText(request.activityArea()));
        company.setLogoUrl(normalizeOptionalText(request.logoUrl()));

        applyUpdatedContactMethods(company, contactMethods);
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
                        "Sirket bulunamadi: " + id
                ));
    }

    private List<NormalizedContactMethodRequest> normalizeCreateContactMethods(
            List<CompanyContactMethodRequest> contactMethods
    ) {
        List<NormalizedContactMethodRequest> normalizedContactMethods =
                normalizeContactMethods(contactMethods);

        for (NormalizedContactMethodRequest contactMethod
                : normalizedContactMethods) {
            if (contactMethod.id() != null) {
                throw new InvalidRequestException(
                        "Yeni iletisim bilgisi icin id gonderilemez"
                );
            }
        }

        return normalizedContactMethods;
    }

    private List<NormalizedContactMethodRequest> normalizeUpdateContactMethods(
            List<CompanyContactMethodRequest> contactMethods
    ) {
        List<NormalizedContactMethodRequest> normalizedContactMethods =
                normalizeContactMethods(contactMethods);

        Set<Long> seenIds = new HashSet<>();
        for (NormalizedContactMethodRequest contactMethod
                : normalizedContactMethods) {
            Long contactMethodId = contactMethod.id();
            if (contactMethodId != null && !seenIds.add(contactMethodId)) {
                throw new InvalidRequestException(
                        "Ayni iletisim bilgisi birden fazla kez gonderilemez"
                );
            }
        }

        return normalizedContactMethods;
    }

    private List<NormalizedContactMethodRequest> normalizeContactMethods(
            List<CompanyContactMethodRequest> contactMethods
    ) {
        if (contactMethods == null || contactMethods.isEmpty()) {
            return List.of();
        }

        List<NormalizedContactMethodRequest> normalizedContactMethods =
                new ArrayList<>();

        for (CompanyContactMethodRequest contactMethod : contactMethods) {
            if (contactMethod == null) {
                throw new InvalidRequestException(
                        "Iletisim bilgisi bos olamaz"
                );
            }

            CompanyContactMethodType type = contactMethod.type();
            if (type == null) {
                throw new InvalidRequestException(
                        "Iletisim tipi zorunludur"
                );
            }

            String value = normalizeRequiredContactMethodValue(
                    contactMethod.value()
            );
            if (CompanyContactMethodType.EMAIL.equals(type)) {
                value = normalizeEmail(value);
                validateEmailFormat(value);
            }

            if (CompanyContactMethodType.PHONE.equals(type)
                    && value.length() > 30) {
                throw new InvalidRequestException(
                        "Telefon en fazla 30 karakter olabilir"
                );
            }

            normalizedContactMethods.add(new NormalizedContactMethodRequest(
                    contactMethod.id(),
                    type,
                    value,
                    normalizeOptionalText(contactMethod.label()),
                    contactMethod.primary()
            ));
        }

        validateAndApplyPrimaryDefaults(normalizedContactMethods);
        validateRequestEmailDuplicates(normalizedContactMethods);

        return normalizedContactMethods;
    }

    private String normalizeRequiredContactMethodValue(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidRequestException(
                    "Iletisim degeri bos birakilamaz"
            );
        }

        return value.trim();
    }

    private void validateEmailFormat(String email) {
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new InvalidRequestException(
                    "Gecerli bir e-posta adresi girilmelidir"
            );
        }
    }

    private void validateAndApplyPrimaryDefaults(
            List<NormalizedContactMethodRequest> contactMethods
    ) {
        Map<CompanyContactMethodType, List<NormalizedContactMethodRequest>>
                contactMethodsByType = new EnumMap<>(
                CompanyContactMethodType.class
        );

        for (NormalizedContactMethodRequest contactMethod : contactMethods) {
            contactMethodsByType
                    .computeIfAbsent(contactMethod.type(), ignored ->
                            new ArrayList<>())
                    .add(contactMethod);
        }

        for (Map.Entry<CompanyContactMethodType,
                List<NormalizedContactMethodRequest>> entry
                : contactMethodsByType.entrySet()) {
            long primaryCount = entry.getValue()
                    .stream()
                    .filter(NormalizedContactMethodRequest::primary)
                    .count();

            if (primaryCount > 1) {
                throw new InvalidRequestException(
                        "Ayni iletisim tipi icin en fazla bir ana bilgi secilebilir"
                );
            }

            if (primaryCount == 0 && !entry.getValue().isEmpty()) {
                NormalizedContactMethodRequest firstContactMethod =
                        entry.getValue().getFirst();
                firstContactMethod.primary(true);
            }
        }
    }

    private void validateRequestEmailDuplicates(
            List<NormalizedContactMethodRequest> contactMethods
    ) {
        Set<String> seenEmails = new LinkedHashSet<>();
        for (NormalizedContactMethodRequest contactMethod : contactMethods) {
            if (!CompanyContactMethodType.EMAIL.equals(contactMethod.type())) {
                continue;
            }

            if (!seenEmails.add(contactMethod.value())) {
                throw new DuplicateResourceException(
                        "Ayni e-posta adresi birden fazla kez gonderilemez"
                );
            }
        }
    }

    private void validateEmailAvailabilityForCreate(
            List<NormalizedContactMethodRequest> contactMethods
    ) {
        for (NormalizedContactMethodRequest contactMethod : contactMethods) {
            if (CompanyContactMethodType.EMAIL.equals(contactMethod.type())
                    && companyContactMethodRepository
                    .existsByTypeAndValueIgnoreCase(
                            CompanyContactMethodType.EMAIL,
                            contactMethod.value()
                    )) {
                throw new DuplicateResourceException(
                        "Bu e-posta adresiyle kayitli bir sirket zaten var"
                );
            }
        }
    }

    private void validateEmailAvailabilityForUpdate(
            List<NormalizedContactMethodRequest> contactMethods,
            Long companyId
    ) {
        for (NormalizedContactMethodRequest contactMethod : contactMethods) {
            if (CompanyContactMethodType.EMAIL.equals(contactMethod.type())
                    && companyContactMethodRepository
                    .existsByTypeAndValueIgnoreCaseAndCompanyIdNot(
                            CompanyContactMethodType.EMAIL,
                            contactMethod.value(),
                            companyId
                    )) {
                throw new DuplicateResourceException(
                        "Bu e-posta adresiyle kayitli baska bir sirket zaten var"
                );
            }
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
                        "Muhatap bilgisi bos olamaz"
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
                        "Muhatap bilgisi bos olamaz"
                );
            }

            Long contactId = contact.id();

            if (contactId != null && !seenIds.add(contactId)) {
                throw new InvalidRequestException(
                        "Ayni muhatap kisi birden fazla kez gonderilemez"
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
                    "En az bir muhatap kisi eklenmelidir"
            );
        }
    }

    private String normalizeRequiredContactName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            throw new InvalidRequestException(
                    "Muhatap adi soyadi bos birakilamaz"
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
                    "Tam olarak bir muhatap kisi birincil olarak secilmelidir"
            );
        }
    }

    private void validateContactMethods(
            List<NormalizedContactRequest> contacts
    ) {
        for (NormalizedContactRequest contact : contacts) {
            if (contact.email() == null && contact.phone() == null) {
                throw new InvalidRequestException(
                        "Muhatap kisi icin e-posta veya telefon bilgilerinden en az biri girilmelidir"
                );
            }
        }
    }

    private void applyUpdatedContactMethods(
            Company company,
            List<NormalizedContactMethodRequest> requests
    ) {
        Map<Long, CompanyContactMethod> contactMethodsById =
                company.getContactMethods()
                        .stream()
                        .filter(contactMethod -> contactMethod.getId() != null)
                        .collect(Collectors.toMap(
                                CompanyContactMethod::getId,
                                Function.identity()
                        ));

        Set<Long> requestedIds = requests.stream()
                .map(NormalizedContactMethodRequest::id)
                .filter(contactMethodId -> contactMethodId != null)
                .collect(Collectors.toSet());

        for (Long contactMethodId : requestedIds) {
            if (!contactMethodsById.containsKey(contactMethodId)) {
                throw new InvalidRequestException(
                        "Iletisim bilgisi bulunamadi veya bu sirkete ait degil: "
                                + contactMethodId
                );
            }
        }

        List<CompanyContactMethod> contactMethodsToRemove =
                company.getContactMethods()
                        .stream()
                        .filter(contactMethod -> contactMethod.getId() != null
                                && !requestedIds.contains(
                                contactMethod.getId()
                        ))
                        .toList();

        contactMethodsToRemove.forEach(company::removeContactMethod);

        Map<NormalizedContactMethodRequest, CompanyContactMethod>
                contactMethodByRequest = new HashMap<>();

        for (NormalizedContactMethodRequest request : requests) {
            CompanyContactMethod contactMethod = request.id() == null
                    ? new CompanyContactMethod()
                    : contactMethodsById.get(request.id());

            applyContactMethod(contactMethod, request, false);

            if (request.id() == null) {
                company.addContactMethod(contactMethod);
            }

            contactMethodByRequest.put(request, contactMethod);
        }

        companyRepository.flush();

        requests.stream()
                .filter(NormalizedContactMethodRequest::primary)
                .map(contactMethodByRequest::get)
                .forEach(contactMethod -> contactMethod.setPrimary(true));
    }

    private void applyContactMethod(
            CompanyContactMethod contactMethod,
            NormalizedContactMethodRequest request
    ) {
        applyContactMethod(contactMethod, request, request.primary());
    }

    private void applyContactMethod(
            CompanyContactMethod contactMethod,
            NormalizedContactMethodRequest request,
            boolean primary
    ) {
        contactMethod.setType(request.type());
        contactMethod.setValue(request.value());
        contactMethod.setLabel(request.label());
        contactMethod.setPrimary(primary);
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
                        "Muhatap kisi bulunamadi veya bu sirkete ait degil: "
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

    private Map<Long, List<CompanyContactMethod>> findContactMethodsByCompanyId(
            List<Company> companies
    ) {
        if (companies.isEmpty()) {
            return Map.of();
        }

        List<Long> companyIds = companies.stream()
                .map(Company::getId)
                .toList();

        return companyContactMethodRepository.findByCompanyIdIn(companyIds)
                .stream()
                .collect(Collectors.groupingBy(
                        contactMethod -> contactMethod.getCompany().getId(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }

    private void validatePagination(int page, int size) {
        if (page < 0) {
            throw new InvalidRequestException(
                    "Sayfa numarasi 0 veya daha buyuk olmalidir"
            );
        }

        if (!ALLOWED_PAGE_SIZES.contains(size)) {
            throw new InvalidRequestException(
                    "Sayfa boyutu yalnizca 10, 15, 20 veya 25 olabilir"
            );
        }
    }

    private CompanyListResponse toListResponse(
            Company company,
            List<CompanyContact> contacts,
            List<CompanyContactMethod> contactMethods
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
                company.getActivityArea(),
                company.getLogoUrl(),
                company.getCompanyType(),
                sortContactMethods(contactMethods).stream()
                        .map(this::toContactMethodResponse)
                        .toList(),
                primaryContact,
                sortedContacts.size()
        );
    }

    private CompanyResponse toResponse(Company company) {
        return new CompanyResponse(
                company.getId(),
                company.getName(),
                company.getActivityArea(),
                company.getLogoUrl(),
                company.getCompanyType(),
                sortContactMethods(company.getContactMethods()).stream()
                        .map(this::toContactMethodResponse)
                        .toList(),
                sortContacts(company.getContacts()).stream()
                        .map(this::toContactResponse)
                        .toList()
        );
    }

    private CompanyContactMethodResponse toContactMethodResponse(
            CompanyContactMethod contactMethod
    ) {
        return new CompanyContactMethodResponse(
                contactMethod.getId(),
                contactMethod.getType(),
                contactMethod.getValue(),
                contactMethod.getLabel(),
                contactMethod.isPrimary()
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

    private List<CompanyContactMethod> sortContactMethods(
            List<CompanyContactMethod> contactMethods
    ) {
        return contactMethods.stream()
                .sorted(Comparator
                        .comparing(
                                CompanyContactMethod::getType,
                                Comparator.nullsLast(Comparator.naturalOrder())
                        )
                        .thenComparing(
                                CompanyContactMethod::isPrimary,
                                Comparator.reverseOrder()
                        )
                        .thenComparing(
                                CompanyContactMethod::getId,
                                Comparator.nullsLast(Long::compareTo)
                        ))
                .toList();
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

    private static final class NormalizedContactMethodRequest {
        private final Long id;
        private final CompanyContactMethodType type;
        private final String value;
        private final String label;
        private boolean primary;

        private NormalizedContactMethodRequest(
                Long id,
                CompanyContactMethodType type,
                String value,
                String label,
                boolean primary
        ) {
            this.id = id;
            this.type = type;
            this.value = value;
            this.label = label;
            this.primary = primary;
        }

        private Long id() {
            return id;
        }

        private CompanyContactMethodType type() {
            return type;
        }

        private String value() {
            return value;
        }

        private String label() {
            return label;
        }

        private boolean primary() {
            return primary;
        }

        private void primary(boolean primary) {
            this.primary = primary;
        }
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
