package com.github.yildizmy.domain;

import com.github.yildizmy.domain.entity.User;
import jakarta.persistence.OneToMany;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserWalletsCascadeTest {

    @Test
    void removingAUserNeverCascadesToWallets() throws Exception {
        OneToMany mapping = User.class.getDeclaredField("wallets").getAnnotation(OneToMany.class);

        assertEquals(0, mapping.cascade().length, "a cascade would delete the other side's transfers with the wallets");
    }
}
