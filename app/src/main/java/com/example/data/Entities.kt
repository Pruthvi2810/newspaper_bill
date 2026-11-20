package com.example.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val address: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "newspapers",
    indices = [Index(value = ["name"], unique = true)]
)
data class NewspaperEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val defaultPrice: Double
)

@Entity(
    tableName = "bills",
    indices = [Index(value = ["billNumber"], unique = true)]
)
data class BillEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val billNumber: String,
    val billCategory: String = CATEGORY_CENTRAL_JAIL,
    val billMonth: Int = 1,
    val billYear: Int = 2026,
    val customerId: Long? = null,
    val customerName: String,
    val customerPhone: String = "",
    val customerAddress: String = "",
    val billDateMillis: Long,
    val grandTotal: Double,
    val paymentStatus: String = PAYMENT_UNPAID,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val PAYMENT_PAID = "PAID"
        const val PAYMENT_UNPAID = "UNPAID"

        const val CATEGORY_CENTRAL_JAIL = "CENTRAL_JAIL"
        const val CATEGORY_IG_OFFICE = "IG_OFFICE"
        const val CATEGORY_CUSTOMER = "CUSTOMER"

        const val CENTRAL_JAIL_FIXED_NAME = "Supritendent of Police,Central Jail Ahmedabad"
        const val CENTRAL_JAIL_GUJARATI_NAME = "અધિક્ષક સાહેબ શ્રી અમદાવાદ મધ્યસ્થ જેલ,અમદાવાદ"
        const val IG_OFFICE_FIXED_NAME = "Office of Inspector General of Gujarat Jails, ahmedabad"
    }
}

@Entity(
    tableName = "bill_items",
    foreignKeys = [
        ForeignKey(
            entity = BillEntity::class,
            parentColumns = ["id"],
            childColumns = ["billId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["billId"])]
)
data class BillItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val billId: Long,
    val newspaperName: String,
    val price1: Double,
    val quantity1: Int,
    val amount1: Double,
    val price2: Double = 0.0,
    val quantity2: Int = 0,
    val amount2: Double = 0.0,
    val itemTotal: Double
)

data class BillWithItems(
    @Embedded val bill: BillEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "billId"
    )
    val items: List<BillItemEntity>
)
