package fu.edu.mss301.digilib.member.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;
@Table("member_profiles")
@Data
@NoArgsConstructor@AllArgsConstructor
@Builder
public class MemberProfile implements Persistable<String> {

    @Id
    private String id; // Matches Keycloak sub field
    private String email;
    @Column("first_name")
    private String firstName;
    @Column("last_name")
    private String lastName;
    private String phone;
    @Column("member_type")
    private String memberType;
    @Column("member_code")
    private String memberCode;
    @Column("borrowing_limit")
    private int borrowingLimit;
    @Column("loan_period_days")
    private int loanPeriodDays;
    @Column("reservation_priority")
    private int reservationPriority;
    @Column("outstanding_balance")
    private BigDecimal outstandingBalance;
    @Column("avatar_key")
    private String avatarKey;
    @Column("created_at")
    private Instant createdAt;
    @Column("updated_at")
    private Instant updatedAt;
    @Column("status")
    private String status;

    @Transient
    @Builder.Default
    private boolean isNewRecord = false;

    @Override
    public boolean isNew() {
        return this.isNewRecord || this.createdAt == null;
    }
}
