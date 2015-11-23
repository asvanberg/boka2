exports = this

class Person
  constructor: (data) ->
    @id = m.prop data.id
    @firstName = m.prop data.firstName
    @lastName = m.prop data.lastName
    @email = m.prop data.email

  @get: (id) ->
    m.secureRequest(
      method: jsRoutes.controllers.Application.specificPerson(id).method
      url: jsRoutes.controllers.Application.specificPerson(id).url
      type: Person
      extract: (xhr) ->
        if xhr.status is 200 then xhr.responseText
        else xhr.status
    )

  @search: (term) ->
    m.secureRequest(
      method: jsRoutes.controllers.Application.search().method
      url: jsRoutes.controllers.Application.search(term).url
      type: Person
    )

exports.Person = Person