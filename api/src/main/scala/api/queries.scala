package api

object queries:

  val allArticles =
    """
      |query articles {
      |  articles {
      |    id
      |    self {
      |      title
      |      subtitle
      |      price
      |    }
      |  }
      |}
      |""".stripMargin

  val addArticle =
    """
      |mutation add {
      |  addArticle(title: "Potrillos", subtitle: "malbec", price: "ARS 800") {
      |    id
      |    self {
      |      title
      |      subtitle
      |      price
      |    }
      |  }
      |}
      |""".stripMargin

  val addArticleToRemove =
    """
      |mutation addBorrar {
      |  addArticle(title: "Potrillos", subtitle: "malbec", price: "ARS 800") {
      |    id
      |  }
      |}
      |""".stripMargin

  val addArticle2 =
    """
      |mutation add2 {
      |  addArticle(title: "Potrillos", subtitle: "pinot noir", price: "ARS 800") {
      |    id
      |  }
      |}
      |""".stripMargin

  val updatePrice =
    """
      |mutation update {
      |  updatePrice(id: "b2c8ccb8-191a-4233-9b34-3e3111a4adaf", newPrice: "ARS 900") {
      |    id
      |    self {
      |      price
      |    }
      |  }
      |}
      |""".stripMargin

  val removeArticle =
    """
      |mutation remove {
      |  removeArticle(value: "014a363e-7a00-48d5-b154-dc024003f3d1") {
      |    id
      |    self {
      |      title
      |      subtitle
      |    }
      |  }
      |}
      |""".stripMargin

  val getAllOrders =
    """
      |query orders {
      |  orders {
      |    id
      |    self {
      |      items {
      |        article {
      |          self {
      |            title
      |          	subtitle
      |          	price
      |          }
      |        }
      |        amount
      |      }
      |      customer {
      |        name
      |        contactInfo
      |        address
      |      }
      |      status
      |    }
      |  }
      |}
      |""".stripMargin

  val placeOrder =
    """
      |mutation place {
      |  placeOrder(
      |    items: [{article: {id: "b2c8ccb8-191a-4233-9b34-3e3111a4adaf", self: {title: "Potrillos", subtitle: "pinot noir", price: "ARS 900"}}, amount: 2}],
      |    customer: {
      |      name: "Martita",
      |      contactInfo: "martita@gmail.com",
      |      address: "Paseo 150 y Avenida 2"
      |    }
      |  ) {
      |    id
      |    self {
      |      items {
      |        article {
      |          id
      |          self {
      |            title
      |            subtitle
      |            price
      |          }
      |        }
      |      }
      |      status
      |      customer {
      |        name
      |        contactInfo
      |        address
      |      }
      |    }
      |  }
      |}
      |""".stripMargin

  val placeOrderToCancel =
    """
      |mutation place {
      |  placeOrder(
      |    items: [{article: {id: "b2c8ccb8-191a-4233-9b34-3e3111a4adaf", self: {title: "Potrillos", subtitle: "pinot noir", price: "ARS 900"}}, amount: 2}],
      |    customer: {
      |      name: "Martita",
      |      contactInfo: "martita@gmail.com",
      |      address: "Paseo 150 y Avenida 2"
      |    }
      |  ) {
      |    id
      |  }
      |}
      |""".stripMargin

  val payOrder =
    """
      |mutation pay {
      |  markAsPaid(value: "1c6c51c9-3d99-4619-889b-73a6c44ca06c") {
      |    id
      |    self {
      |      status
      |    }
      |  }
      |}
      |""".stripMargin

  val deliverOrder =
    """
      |mutation deliver {
      |  markAsDelivered(value: "1c6c51c9-3d99-4619-889b-73a6c44ca06c") {
      |    id
      |    self {
      |      status
      |    }
      |  }
      |}
      |""".stripMargin

  val cancelOrder =
    """
      |mutation cancel {
      |  markAsCancelled(value: "0d3d6d22-75c4-43f1-bf57-44679ed56d65") {
      |    id
      |  }
      |}
      |""".stripMargin

  val getStock =
    """
      |query stock {
      |  stock(value: "b2c8ccb8-191a-4233-9b34-3e3111a4adaf") {
      |    id
      |    self {
      |      inStock
      |      compromised
      |    }
      |  }
      |}
      |""".stripMargin

  val writeStock =
    """
      |mutation stockWrite {
      |  overwriteStock(id: "b2c8ccb8-191a-4233-9b34-3e3111a4adaf", amount: 12) {
      |    id
      |    self {
      |      inStock
      |      compromised
      |    }
      |  }
      |}
      |""".stripMargin

  val incrementStock =
    """
      |mutation stockIncrement {
      |  incrementStock(id: "b2c8ccb8-191a-4233-9b34-3e3111a4adaf", amount: 6) {
      |    id
      |    self {
      |      inStock
      |      compromised
      |    }
      |  }
      |}
      |""".stripMargin
