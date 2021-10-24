package com.t1908e.memeportalapi.dto;

import com.t1908e.memeportalapi.entity.Invoice;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;

@Getter
@Service
@AllArgsConstructor
@NoArgsConstructor
public class InvoiceDTO {
    private int id;
    private String name;
    private String content;
    private double amount;
    private Date createdAt;
    private Date updatedAt;
    private int status;

    public InvoiceDTO(Invoice invoice) {
        this.id = invoice.getId();
        this.name = invoice.getName();
        this.content = invoice.getContent();
        this.amount = invoice.getAmount();
        this.createdAt = invoice.getCreatedAt();
        this.updatedAt = invoice.getUpdatedAt();
        this.status = invoice.getStatus();
    }
}
