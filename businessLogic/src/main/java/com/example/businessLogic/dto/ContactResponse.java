package com.example.businessLogic.dto;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContactResponse {
    private Long contactId;
    private String contactUserId;
    private String contactUserName;
    private Boolean isPinned;
}