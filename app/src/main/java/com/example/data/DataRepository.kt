package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class DataRepository(private val database: AppDatabase) {
    private val callLogDao = database.callLogDao()
    private val faqDao = database.faqDao()
    private val priorityContactDao = database.priorityContactDao()

    // --- REACTIVE FLOWS FOR UI LAYER (AUTO-DECRYPTED) ---

    val allCallLogs: Flow<List<DecryptedCallLog>> = callLogDao.getAllCallLogs()
        .map { entities -> entities.map { it.toDecrypted() } }

    val allFaqs: Flow<List<DecryptedFaq>> = faqDao.getAllFaqs()
        .map { entities -> entities.map { it.toDecrypted() } }

    val allPriorityContacts: Flow<List<DecryptedPriorityContact>> = priorityContactDao.getAllPriorityContacts()
        .map { entities -> entities.map { it.toDecrypted() } }

    // --- WRITES & DIRECT READS (RUNS ON IO DISPATCHER) ---

    suspend fun insertCallLog(log: DecryptedCallLog) = withContext(Dispatchers.IO) {
        callLogDao.insertCallLog(log.toEntity())
    }

    suspend fun deleteCallLog(id: Int) = withContext(Dispatchers.IO) {
        callLogDao.deleteCallLogById(id)
    }

    suspend fun clearAllCallLogs() = withContext(Dispatchers.IO) {
        callLogDao.clearAllCallLogs()
    }

    suspend fun insertFaq(faq: DecryptedFaq) = withContext(Dispatchers.IO) {
        faqDao.insertFaq(faq.toEntity())
    }

    suspend fun deleteFaq(id: Int) = withContext(Dispatchers.IO) {
        faqDao.deleteFaqById(id)
    }

    suspend fun getEnabledFaqs(): List<DecryptedFaq> = withContext(Dispatchers.IO) {
        faqDao.getEnabledFaqs().map { it.toDecrypted() }
    }

    suspend fun insertPriorityContact(contact: DecryptedPriorityContact) = withContext(Dispatchers.IO) {
        priorityContactDao.insertPriorityContact(contact.toEntity())
    }

    suspend fun deletePriorityContact(id: Int) = withContext(Dispatchers.IO) {
        priorityContactDao.deletePriorityContactById(id)
    }

    suspend fun getEnabledPriorityContacts(): List<DecryptedPriorityContact> = withContext(Dispatchers.IO) {
        priorityContactDao.getEnabledPriorityContacts().map { it.toDecrypted() }
    }
}
