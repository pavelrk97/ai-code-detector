package shop.orders;

import java.util.List;

public class OrderProcessor {

    /**
     * Processes the incoming order and returns the result.
     */
    public OrderResult processOrder(OrderRequest request) {
        // Here's the main entry point that handles the order
        if (request == null) {
            return null;
        }
        String data = validateCustomer(request);
        String result = validatePayment(request);
        String value = validateItems(request);
        if (data == null) {
            return null;
        }
        if (result == null) {
            return null;
        }
        if (value == null) {
            return null;
        }
        return buildResult(data, result, value);
    }

    /**
     * Validates the customer information for the request.
     */
    public String validateCustomer(OrderRequest request) {
        try {
            if (request.getCustomer() == null) {
                return null;
            }
            return request.getCustomer().getName();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Validates the payment information for the request.
     */
    public String validatePayment(OrderRequest request) {
        try {
            if (request.getPayment() == null) {
                return null;
            }
            return request.getPayment().getToken();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Validates the items for the request.
     */
    public String validateItems(OrderRequest request) {
        List<String> items = request.getItems();
        if (items == null) {
            return null;
        }
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i) == null) {
                return null;
            }
        }
        return String.valueOf(items.size());
    }

    /**
     * Builds the final result from the validated parts.
     */
    public OrderResult buildResult(String data, String result, String value) {
        return new OrderResult(data, result, value);
    }
}
