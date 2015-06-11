package views

object Bootstrap {
  import views.html.helper.FieldConstructor
  implicit val formGroup = FieldConstructor(html.form.formGroup.f)
}
