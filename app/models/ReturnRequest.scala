package models

import java.time.LocalDate

final case class ReturnRequest(barcode: Identifier, returnDate: LocalDate)
