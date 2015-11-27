layout = (component) ->
  {
    controller: () -> new layout.controller(component)
    view: layout.view
  }

layout.controller = (component) ->
  content: component.view.bind(this, new component.controller)

layout.view = (ctrl) ->
  m(".container", [
    m(".row", [
      m(".col-sm-9.animated.fadeIn", {style: {"z-index": 100}}, ctrl.content())
      m(".col-sm-3", [
        m("h4", "Products")
        m("ul", [
          m("li", m("a[href='/product']", {config: m.route}, "Product list"))
          m("li", m("a[href='/product/add']", {config: m.route}, "Add product"))
          m("li", m("a[href='/person']", {config: m.route}, "Person"))
          m("li", m("a[href='/inventoryCheck']", {config: m.route}, "Inventory check"))
          m("li", m("a[href='/loan/record']", {config: m.route}, "Record loan"))
          m("li", m("a[href='/loan/return']", {config: m.route}, "Return items"))
        ])
      ])
    ])
  ])

login = ->
  m "p", [
    "Please "
    m "a", {href: "https://#{auth0.domain}/authorize?response_type=token&scope=openid%20name%20email&connection=DSV&client_id=#{auth0.clientId}&redirect_uri=#{auth0.callbackUrl}&state=#{m.route.param("return")}"}, "log in"
    "."
  ]

forbidden = ->
  m "p", "You do not belong here."

exports = this

document.addEventListener "DOMContentLoaded", () ->
  m.route(document.body, "/inventoryCheck", {
    "/person": layout(exports.person.search)
    "/person/:id": layout(exports.person.view)
    "/inventoryCheck": layout(exports.inventoryCheck)
    "/product/add": layout(exports.product.add)
    "/product": layout(exports.product.list)
    "/product/:id": layout(exports.product.view)
    "/loan/record": layout(exports.loan.recordLoan)
    "/loan/return": layout(exports.loan.returnLoan)
    "/login": view: login
    "/forbidden": view: forbidden
  })