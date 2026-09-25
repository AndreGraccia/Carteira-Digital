package com.github.yildizmy.domain;

import com.github.yildizmy.domain.entity.Transaction;
import com.github.yildizmy.domain.entity.Type;
import com.github.yildizmy.domain.entity.Wallet;
import jakarta.persistence.OneToMany;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** A transaction belongs to both sides of a transfer: removing a wallet or a type must never cascade to it. */
class TransactionOwnershipTest {

    @Test
    void noEntityCascadesToTransactions() {
        for (Class<?> owner : new Class<?>[]{Wallet.class, Type.class}) {
            for (Field field : owner.getDeclaredFields()) {
                OneToMany relation = field.getAnnotation(OneToMany.class);
                if (relation == null) continue;
                if (!(field.getGenericType() instanceof ParameterizedType type)) continue;
                if (type.getActualTypeArguments()[0] != Transaction.class) continue;
                assertEquals("[]", Arrays.toString(relation.cascade()), owner.getSimpleName() + "." + field.getName());
            }
        }
    }
}
