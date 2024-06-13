package com.hkshenoy.jaltantraloopsb.security;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name="users_session_details")
public class UserSessionDetail {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false)
    private String sessionId;

    @Column(nullable=false)
    private String loginDatetime;

    @Column(nullable=false)
    private String clientIpAddress;

}
