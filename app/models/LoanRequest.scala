package models

import java.time.LocalDate

final case class LoanRequest(borrower: PersonId, barcode: Identifier, borrowed: LocalDate)
