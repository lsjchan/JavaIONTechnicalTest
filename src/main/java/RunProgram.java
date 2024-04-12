public class RunProgram {
    public static void main(String[] args) throws InterruptedException {
        // NOTE: OrderMatchingEngineTest.java contains everything about part A

        OrderMatchingEngine.getInstance().reset();

        // send multiple buy orders
        Order orderBuy1 = new Order(1, 4, Helper.SIDE_BUY, 100);
        Order orderBuy2 = new Order(2, 5.5, Helper.SIDE_BUY, 100);
        Order orderBuy3 = new Order(3, 6, Helper.SIDE_BUY, 100);

        OrderMatchingEngine orderMatchingEngine = OrderMatchingEngine.getInstance();
        orderMatchingEngine.processNewOrderSingle(orderBuy1);
        orderMatchingEngine.processNewOrderSingle(orderBuy2);
        orderMatchingEngine.processNewOrderSingle(orderBuy3);

        // send a sell order
        Order orderSell1 = new Order(4, 5.4, Helper.SIDE_SELL, 140);

        orderMatchingEngine.processNewOrderSingle(orderSell1);


        // send a cancel request
        CancelRequest orderCancel1 = new CancelRequest(1);
        orderMatchingEngine.processCancelRequest(orderCancel1);


        // send replace requests
        ReplaceRequest replaceRequest1 = new ReplaceRequest(1, 200);
        ReplaceRequest replaceRequest2 = new ReplaceRequest(2, 400);

        orderMatchingEngine.processReplaceRequest(replaceRequest1);
        orderMatchingEngine.processReplaceRequest(replaceRequest2);

        OrderMatchingEngine.getInstance().reset();
    }
}
