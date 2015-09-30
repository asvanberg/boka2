class Product
  constructor: (data) ->
    @id = m.prop data?.id
    @name = m.prop data?.name
    @description = m.prop data?.description

  @list: ->
    m.request
      method: jsRoutes.controllers.Application.listProducts().method
      url: jsRoutes.controllers.Application.listProducts().url
      type: Product
      background: true

productModule =
  add:
    controller: () ->
      @error = m.prop {}
      @product = m.prop new Product
      @add = (product) =>
        m.request
          method: jsRoutes.controllers.Application.doAddProduct().method
          url: jsRoutes.controllers.Application.doAddProduct().url
          data: product
          type: Product
        .then @product, @error
      return @
    view: (ctrl) ->
      [
        m "h1", "Add product"
        m.component productModule.form, {error: ctrl.error, onsave: ctrl.add}
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


  form:
    controller: (args) ->
      @product = args.product || m.prop new Product
      @error = args.error || m.prop {}
      return @
    view: (ctrl, args) ->
      product = ctrl.product()
      m "form", {onsubmit: (e) -> e.preventDefault(); args.onsave(product)}, [
        if ctrl.error().fields then m ".alert.alert-danger", [
          m "ul", [m "li", message for _, message of ctrl.error().fields]
        ]
        m ".form-group", {class: if ctrl.error().fields?.name then "has-error" else ""}, [
          m "label[for=name].control-label", "Name"
          m "input#name[type=text][].form-control", {oninput: m.withAttr("value", product.name)}
        ]
        m ".form-group", {class: if ctrl.error().fields?.description then "has-error" else ""}, [
          m "label[for=description].control-label", "Description"
          m "textarea#description.form-control", {oninput: m.withAttr("value", product.description)}
        ]
        m "button[type=submit].btn.btn-primary", "Save"
      ]

exports = this
exports.product =
  add: productModule.add
  list: productModule.list