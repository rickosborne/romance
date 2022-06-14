package org.rickosborne.audiobookstore.client.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.ToString;

import java.util.List;
import java.util.UUID;

@Data
@ToString(exclude = "audiobooks")
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserInformation2 {
    @JsonProperty("Audiobooks")
    List<BookInformation> audiobooks;
    @JsonProperty("CancelledMember")
    Boolean cancelledMember;
    @JsonProperty("IsCorporate")
    Boolean corporate;
    @JsonProperty("CustomerNumber")
    String customerNumber;
    @JsonProperty("CustomerRank")
    Integer customerRank;
    @JsonProperty("EmailAddress")
    String emailAddress;
    @JsonProperty("FirstName")
    String firstName;
    @JsonProperty("IsFLexpassMember")
    Boolean flexpassMember;
    @JsonProperty("LastName")
    String lastName;
    @JsonProperty("NumOfBooksInLibrary")
    Integer numOfBooksInLibrary;

    @JsonProperty("NumOfDaysAsMember")
    Integer numOfDaysAsMember;
    @JsonProperty("OrderTotalAmount")
    Double orderTotalAmount;
    @JsonProperty("UpdateTime")
    Long updateTime;
    @JsonProperty("UserId")
    UUID userId;
}
