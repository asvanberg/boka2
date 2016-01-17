package se.su.dsv.boka2.api.model

import java.time.LocalDate

import models.{Identifier, PersonId}

final case class LoanRequest(borrower: PersonId, barcode: Identifier, borrowed: LocalDate)
