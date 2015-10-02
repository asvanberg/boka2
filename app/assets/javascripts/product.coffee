class Product
  constructor: (data) ->
    @id = m.prop data?.id
    @name = m.prop data?.name or ""
    @description = m.prop data?.description or ""

  @list: ->
    m.request
      method: jsRoutes.controllers.Application.listProducts().method
      url: jsRoutes.controllers.Application.listProducts().url
      type: Product
      background: true

  @save: (product) ->
    m.request
      method: jsRoutes.controllers.Application.doEditProduct(product.id()).method
      url: jsRoutes.controllers.Application.doEditProduct(product.id()).url
      data: product
      type: Product

class Copy
  constructor: (data) ->
    @barcode = m.prop data.barcode
    @note = m.prop data.note

class ProductDetails
  constructor: (data) ->
    @product = m.prop new Product data.product
    @copies = m.prop (new Copy copy for copy in data.copies)

  @get: (id) ->
    m.request
      method: jsRoutes.controllers.Application.viewProduct(id).method
      url: jsRoutes.controllers.Application.viewProduct(id).url
      type: ProductDetails

productModule =
  add:
    controller: () ->
      @product = m.prop new Product
      @add = (product) ->
        m.request(
          method: jsRoutes.controllers.Application.doAddProduct().method
          url: jsRoutes.controllers.Application.doAddProduct().url
          data: product
          type: Product
        ).then (product) -> m.route "/product/#{product.id()}"
      return
    view: (ctrl) ->
      [
        m "h1", "Add product"
        m.component form, {onsave: ctrl.add}
      ]
  list:
    controller: ->
      productList = Product.list()
      productList.then(m.redraw)
      productList: productList
    view: (ctrl) ->
      [
        m "h1", "Product list"
        m "table.table.table-hover.table-striped", [
          m "thead", [
            m "tr", [
              m "th", "Name"
            ]
          ]
          m "tbody", [
            if not ctrl.productList() then m "tr", m "td[colspan=1].text-center", "Loading..."
            else [
              ctrl.productList().map (product) ->
                m "tr", m "td", m "a[href='/product/#{product.id()}']", {config: m.route}, product.name()
              m "tr", m "td[colspan=1].text-center", "No products" unless ctrl.productList().length
            ]
          ]
        ]
      ]
  view:
    controller: ->
      productDetails = ProductDetails.get m.route.param "id"
      productDetails.then null, m.route.bind this, "/product"
      editing = m.prop false

      productDetails: productDetails
      editing: editing
      save: (product) ->
        Product.save product
          .then(
            (product) ->
              productDetails().product(product)
              editing(false)
          )
    view: (ctrl) ->
      product = ctrl.productDetails().product
      [
        m "h1", "Product details"
        m.component form, {product: product, onsave: ctrl.save, oncancel: -> m.route "/product/#{product().id()}"} if ctrl.editing()
        m "form", [
          m "a.btn.btn-link.pull-right", {onclick: ctrl.editing.bind this, true}, "Edit"
          m ".form-group", [
            m "label.control-label", "Name"
            m "p.form-control-static", product().name()
          ]
          m ".form-group", [
            m "label.control-label", "Description"
            m "p.form-control-static", product().description() or m "i", "None"
          ]
        ] unless ctrl.editing()
      ]

form =
  controller: (args) ->
    @product = args.product or m.prop new Product
    @error = m.prop {}
    return @
  view: (ctrl, args) ->
    product = ctrl.product()
    m "form", {onsubmit: (e) -> e.preventDefault(); args.onsave(product).then(null, ctrl.error)}, [
      if ctrl.error().fields then m ".alert.alert-danger", [
        m "ul", [m "li", message for _, message of ctrl.error().fields]
      ]
      m ".form-group", {class: if ctrl.error().fields?.name then "has-error" else ""}, [
        m "label[for=name].control-label", "Name"
        m "input#name[type=text][required].form-control", {oninput: m.withAttr("value", product.name), value: product.name()}
      ]
      m ".form-group", {class: if ctrl.error().fields?.description then "has-error" else ""}, [
        m "label[for=description].control-label", "Description"
        m "textarea#description.form-control", {oninput: m.withAttr("value", product.description), value: product.description()}
      ]
      m "button[type=submit].btn.btn-primary", "Save"
      m "a.btn.btn-link", {onclick: args.oncancel}, "Cancel" if args.oncancel
    ]

exports = this
exports.product = productModule