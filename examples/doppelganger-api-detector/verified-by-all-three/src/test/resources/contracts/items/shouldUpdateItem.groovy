import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "updates an item by id - a concrete example of the templated PUT /items/{id} endpoint"
    request {
        method 'PUT'
        url '/items/1'
        body([name: "Widget"])
        headers {
            contentType('application/json')
        }
    }
    response {
        status 200
    }
}
