package se.su.dsv.boka2.api.model

import java.time.LocalDate

import models.Identifier

final case class ReturnRequest(barcode: Identifier, returnDate: LocalDate)
