package com.example.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow

class BillingRepository(private val database: AppDatabase) {
    private val customerDao = database.customerDao()
    private val newspaperDao = database.newspaperDao()
    private val billDao = database.billDao()

    val allCustomers: Flow<List<CustomerEntity>> = customerDao.getAllCustomers()
    val allNewspapers: Flow<List<NewspaperEntity>> = newspaperDao.getAllNewspapers()
    val allBillsWithItems: Flow<List<BillWithItems>> = billDao.getAllBillsWithItems()

    suspend fun seedDefaultNewspapersIfEmpty() {
        if (newspaperDao.getCount() == 0) {
            val defaults = listOf(
                NewspaperEntity(name = "Gujarat Samachar", defaultPrice = 5.0),
                NewspaperEntity(name = "Sandesh", defaultPrice = 5.0),
                NewspaperEntity(name = "Divya Bhaskar", defaultPrice = 5.0),
                NewspaperEntity(name = "Times Of India", defaultPrice = 5.0),
                NewspaperEntity(name = "Indian Express", defaultPrice = 5.0),
                NewspaperEntity(name = "Jay Hind", defaultPrice = 5.0),
                NewspaperEntity(name = "Rajsthan Patrika", defaultPrice = 5.0),
                NewspaperEntity(name = "Phool-Chhab", defaultPrice = 5.0),
                NewspaperEntity(name = "Nav Gujarat Samay", defaultPrice = 5.0),
                NewspaperEntity(name = "India Today Magazine", defaultPrice = 125.0)
            )
            defaults.forEach { newspaperDao.insertNewspaper(it) }
        }
    }

    suspend fun insertCustomer(customer: CustomerEntity): Long = customerDao.insertCustomer(customer)
    suspend fun updateCustomer(customer: CustomerEntity) = customerDao.updateCustomer(customer)
    suspend fun deleteCustomer(customer: CustomerEntity) = customerDao.deleteCustomer(customer)

    suspend fun insertNewspaper(newspaper: NewspaperEntity): Long = newspaperDao.insertNewspaper(newspaper)
    suspend fun updateNewspaper(newspaper: NewspaperEntity) = newspaperDao.updateNewspaper(newspaper)
    suspend fun deleteNewspaper(newspaper: NewspaperEntity) = newspaperDao.deleteNewspaper(newspaper)

    /**
     * Central Jail Bill Number Pattern:
     * 4 digits where first two digits are always 10 and last two digits are the selected month (01..12),
     * followed by /<currentYear>, e.g. October 2026 -> 1010/2026
     */
    fun formatCentralJailBillNumber(month: Int, year: Int): String {
        val monthTwoDigits = month.coerceIn(1, 12).toString().padStart(2, '0')
        return "10$monthTwoDigits/$year"
    }

    /**
     * IG Office Bill Number Pattern:
     * 4 digits where first two digits are always 20 and last two digits are the selected month (01..12),
     * followed by /<currentYear>, e.g. October 2026 -> 2010/2026
     */
    fun formatIgOfficeBillNumber(month: Int, year: Int): String {
        val monthTwoDigits = month.coerceIn(1, 12).toString().padStart(2, '0')
        return "20$monthTwoDigits/$year"
    }

    suspend fun generateNextBillNumber(category: String, month: Int, year: Int): String {
        if (category == BillEntity.CATEGORY_CENTRAL_JAIL) {
            return formatCentralJailBillNumber(month, year)
        }
        if (category == BillEntity.CATEGORY_IG_OFFICE) {
            return formatIgOfficeBillNumber(month, year)
        }
        val prefix = "CUST-"
        val existingNumbers = billDao.getAllBillNumbers()
        var maxNum = 1000
        for (numStr in existingNumbers) {
            if (numStr.startsWith(prefix)) {
                val digits = numStr.removePrefix(prefix).filter { it.isDigit() }.toIntOrNull()
                if (digits != null && digits > maxNum) {
                    maxNum = digits
                }
            }
        }
        var candidate = "$prefix${maxNum + 1}"
        var offset = 2
        while (billDao.getBillByNumber(candidate) != null) {
            candidate = "$prefix${maxNum + offset}"
            offset++
        }
        return candidate
    }

    suspend fun isBillNumberDuplicate(billNumber: String, excludeBillId: Long? = null): Boolean {
        val existing = billDao.getBillByNumber(billNumber.trim()) ?: return false
        return excludeBillId == null || existing.id != excludeBillId
    }

    suspend fun saveBillWithItems(
        bill: BillEntity,
        items: List<BillItemEntity>
    ): Result<BillWithItems> {
        val trimmedNumber = bill.billNumber.trim()
        if (trimmedNumber.isEmpty()) {
            return Result.failure(IllegalArgumentException("Bill number cannot be empty."))
        }
        val duplicate = isBillNumberDuplicate(trimmedNumber, if (bill.id == 0L) null else bill.id)
        if (duplicate) {
            return Result.failure(IllegalArgumentException("Bill number \"$trimmedNumber\" already exists."))
        }

        val savedId = database.withTransaction {
            val cleanBill = bill.copy(
                billNumber = trimmedNumber,
                updatedAt = System.currentTimeMillis()
            )
            val billId = if (cleanBill.id == 0L) {
                billDao.insertBill(cleanBill)
            } else {
                billDao.updateBill(cleanBill)
                billDao.deleteBillItemsByBillId(cleanBill.id)
                cleanBill.id
            }
            val linkedItems = items.map { it.copy(id = 0, billId = billId) }
            billDao.insertBillItems(linkedItems)
            billId
        }

        val result = billDao.getBillWithItemsById(savedId)
            ?: return Result.failure(IllegalStateException("Failed to load saved bill."))
        return Result.success(result)
    }

    suspend fun deleteBill(billId: Long) = billDao.deleteBillById(billId)

    suspend fun updatePaymentStatus(billId: Long, status: String) {
        billDao.updatePaymentStatus(billId, status)
    }
}
