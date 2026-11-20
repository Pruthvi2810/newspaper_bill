package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY name COLLATE NOCASE ASC")
    fun getAllCustomers(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun getCustomerById(id: Long): CustomerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: CustomerEntity): Long

    @Update
    suspend fun updateCustomer(customer: CustomerEntity)

    @Delete
    suspend fun deleteCustomer(customer: CustomerEntity)
}

@Dao
interface NewspaperDao {
    @Query("SELECT * FROM newspapers ORDER BY name COLLATE NOCASE ASC")
    fun getAllNewspapers(): Flow<List<NewspaperEntity>>

    @Query("SELECT COUNT(*) FROM newspapers")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNewspaper(newspaper: NewspaperEntity): Long

    @Update
    suspend fun updateNewspaper(newspaper: NewspaperEntity)

    @Delete
    suspend fun deleteNewspaper(newspaper: NewspaperEntity)
}

@Dao
interface BillDao {
    @Transaction
    @Query("SELECT * FROM bills ORDER BY billDateMillis DESC, id DESC")
    fun getAllBillsWithItems(): Flow<List<BillWithItems>>

    @Transaction
    @Query("SELECT * FROM bills WHERE id = :billId")
    suspend fun getBillWithItemsById(billId: Long): BillWithItems?

    @Query("SELECT * FROM bills WHERE billNumber = :billNumber LIMIT 1")
    suspend fun getBillByNumber(billNumber: String): BillEntity?

    @Query("SELECT billNumber FROM bills")
    suspend fun getAllBillNumbers(): List<String>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertBill(bill: BillEntity): Long

    @Update
    suspend fun updateBill(bill: BillEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBillItems(items: List<BillItemEntity>)

    @Query("DELETE FROM bill_items WHERE billId = :billId")
    suspend fun deleteBillItemsByBillId(billId: Long)

    @Query("DELETE FROM bills WHERE id = :billId")
    suspend fun deleteBillById(billId: Long)

    @Query("UPDATE bills SET paymentStatus = :status, updatedAt = :updatedAt WHERE id = :billId")
    suspend fun updatePaymentStatus(billId: Long, status: String, updatedAt: Long = System.currentTimeMillis())
}
