package com.insurance.newbusiness.integration.model.pas;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PolicyCheckIn {

    private List<BasicPolicyInsured> basicPolicyInsured;
    private ImportantDates importantDates;
    private ProductSelectionDetails productSelection;
    private String source;
    private Object additionalInfoMap;
    private List<IntegrationDetail> integrations;
    private BankDetailsDTO bankDetailsDTO;
    private BiSummary biSummary;
    private List<DeclarationUnderFATCADTO> declarationUnderFATCADTO;
    private EiaDTO eiadto;
    private IpFamilyDetailsDTO ipFamilyDetailsDTO;
    private HabbitDetailsDTO habbitDetailsDTO;
    private IccrDTO iccrdto;
    private IpDetails ipDetails;
    private KycAmlAndOtherProofsDTO kycamlAndOtherProofsDTO;
    private List<NomineeAndAppointeeDetailsDTO> nomineeAndAppointeeDetailsDTO;
    private List<Object> otherInsDetailsDTO;
    private PayerDetails payerDetails;
    private PhDetails phDetails;
    private PolicySelectionDetails policySelection;
    private PremCollDetails premCollDetails;
    private ProductDetailsDTO productDetailsDTO;
    private Object thirdPartyPaymentDetailsDTO;
    private TopUpDetailsDTO topUpDetailsDTO;
    private WitnessDetailsDTO witnessDetailsDTO;
    private Object phFamilyDetailsDTO;
    private Object scrutinyDetails;
    private List<QuestionnaireDetails> questionnaireDetails;
    private Object customers;
    private JourneyDetails journeyDetails;
    private PasaDetails pasaDetails;
    private AdditionalDetails additionalDetails;
    private Object ftDetails;
    private Object jointLifeInsuredDetails;
    private Boolean appGeneratedByNb;
}
