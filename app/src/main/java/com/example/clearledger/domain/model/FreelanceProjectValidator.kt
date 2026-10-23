package com.example.clearledger.domain.model

object FreelanceProjectValidator {

    private val supportedCurrencies = setOf(
        "LKR", "USD", "EUR", "GBP", "AUD", "INR"
    )

    fun validate(
        name: String,
        client: String,
        agreedAmount: String,
        currency: String,
        rate: String
    ): String? {
        if (name.isBlank()) {
            return "Please enter a project name."
        }

        if (client.isBlank()) {
            return "Please enter the client name."
        }

        val amount = agreedAmount.toDoubleOrNull()
        if (amount == null || !amount.isFinite() || amount <= 0.0) {
            return "Enter an agreed amount greater than zero."
        }

        if (currency.trim().uppercase() !in supportedCurrencies) {
            return "Choose a supported currency: LKR, USD, EUR, GBP, AUD or INR."
        }

        val exchangeRate = rate.toDoubleOrNull()
        if (exchangeRate == null ||
            !exchangeRate.isFinite() ||
            exchangeRate <= 0.0
        ) {
            return "Enter an exchange rate greater than zero."
        }

        if (currency.trim().uppercase() == "LKR" && exchangeRate != 1.0) {
            return "For LKR, the exchange rate must be 1."
        }

        return null
    }
}