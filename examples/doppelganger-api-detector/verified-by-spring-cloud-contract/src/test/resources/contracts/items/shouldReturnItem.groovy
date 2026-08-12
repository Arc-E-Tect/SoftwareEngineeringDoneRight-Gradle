import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "returns an item by id - a concrete example of the templated GET /items/{id} endpoint"
    request {
        method 'GET'
        url '/items/1'
    }
    response {
        status 200
        body([:])
    }
}
