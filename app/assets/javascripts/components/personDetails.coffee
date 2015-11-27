exports = this

personDetails =
  controller: ->
  view: (_, person) -> m "div", [
    m.component exports.components.image.person,
      class: "pull-right img-rounded"
      person: person()
    m "dl", [
      m "dt", "First name"
      m "dd", person().firstName()
      m "dt", "Last name"
      m "dd", person().lastName()
      m "dt", "Email"
      m "dd", person().email()
    ]
  ]

exports.components ||= {}
exports.components.details ||= {}
exports.components.details.person = personDetails