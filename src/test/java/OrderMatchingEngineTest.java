import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.List;

public class OrderMatchingEngineTest {

    @Before
    public void setUp(){
        OrderMatchingEngine.getInstance().reset();
    }

    @Test
    public void testSendingSellLimitOrderInABidDominatedBook() throws InterruptedException { // Given an Order, add it to the OrderBook (order additions are expected to occur extremely frequently)

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

        ProcessNewOrderSingleResult orderMessageSell1ProcessNewOrderSingleResult = orderMatchingEngine.processNewOrderSingle(orderSell1);

        Assert.assertEquals(OrdStatus.FILLED, orderSell1.getOrdStatus());
        Assert.assertEquals(140, orderSell1.getCumQty());
        Assert.assertEquals(0, orderSell1.getLeavesQty());
        Assert.assertEquals(ProcessNewOrderSingleResult.FULLY_FILLED, orderMessageSell1ProcessNewOrderSingleResult);

        Assert.assertEquals(OrdStatus.FILLED, orderBuy3.getOrdStatus());
        Assert.assertEquals(100, orderBuy3.getCumQty());
        Assert.assertEquals(0, orderBuy3.getLeavesQty());

        Assert.assertEquals(OrdStatus.PARTIALLY_FILLED, orderBuy2.getOrdStatus());
        Assert.assertEquals(40, orderBuy2.getCumQty());
        Assert.assertEquals(60, orderBuy2.getLeavesQty());

        OrderMatchingEngine.getInstance().reset();
    }

    @Test
    public void testSendingBuyLimitOrderInASellDominatedBook() throws InterruptedException { // Given an Order, add it to the OrderBook (order additions are expected to occur extremely frequently)

        OrderMatchingEngine.getInstance().reset();

        // send multiple sell orders
        Order orderSell1 = new Order(1, 4, Helper.SIDE_SELL, 100);
        Order orderSell2 = new Order(2, 5.5, Helper.SIDE_SELL, 100);
        Order orderSell3 = new Order(3, 6, Helper.SIDE_SELL, 100);

        OrderMatchingEngine orderMatchingEngine = OrderMatchingEngine.getInstance();
        orderMatchingEngine.processNewOrderSingle(orderSell1);
        orderMatchingEngine.processNewOrderSingle(orderSell3);
        orderMatchingEngine.processNewOrderSingle(orderSell2);

        // send a buy order
        final String clientB = "clientB";
        Order orderBuy4 = new Order(4, 5.6, Helper.SIDE_BUY, 140);

        ProcessNewOrderSingleResult orderMessageBuy1ProcessNewOrderSingleResult = orderMatchingEngine.processNewOrderSingle(orderBuy4);

        Assert.assertEquals(OrdStatus.FILLED, orderBuy4.getOrdStatus());
        Assert.assertEquals(140, orderBuy4.getCumQty());
        Assert.assertEquals(0, orderBuy4.getLeavesQty());
        Assert.assertEquals(ProcessNewOrderSingleResult.FULLY_FILLED, orderMessageBuy1ProcessNewOrderSingleResult);

        Assert.assertEquals(OrdStatus.FILLED, orderSell1.getOrdStatus());
        Assert.assertEquals(100, orderSell1.getCumQty());
        Assert.assertEquals(0, orderSell1.getLeavesQty());

        Assert.assertEquals(OrdStatus.PARTIALLY_FILLED, orderSell2.getOrdStatus());
        Assert.assertEquals(40, orderSell2.getCumQty());
        Assert.assertEquals(60, orderSell2.getLeavesQty());

        Assert.assertEquals(2, OrderMatchingEngine.getInstance().getAskPriceLevels().values().stream().mapToInt(LinkedHashMap::size).sum()); // verify "bidPriceLevels" only contains 2 orders


        // send a buy order
        Order orderBuy5 = new Order(5, 5.5, Helper.SIDE_BUY, 200);

        ProcessNewOrderSingleResult orderMessageBuy2ProcessNewOrderSingleResult = orderMatchingEngine.processNewOrderSingle(orderBuy5);

        Assert.assertEquals(OrdStatus.PARTIALLY_FILLED, orderBuy5.getOrdStatus());
        Assert.assertEquals(60, orderBuy5.getCumQty());
        Assert.assertEquals(140, orderBuy5.getLeavesQty());
        Assert.assertEquals(ProcessNewOrderSingleResult.PARTIALLY_FILLED, orderMessageBuy2ProcessNewOrderSingleResult);

        Assert.assertEquals(OrdStatus.FILLED, orderSell2.getOrdStatus());
        Assert.assertEquals(100, orderSell2.getCumQty());
        Assert.assertEquals(0, orderSell2.getLeavesQty());


        // send a buy order
        Order orderBuy6 = new Order(6, 6, Helper.SIDE_BUY, 180);

        ProcessNewOrderSingleResult orderMessageBuy3ProcessNewOrderSingleResult = orderMatchingEngine.processNewOrderSingle(orderBuy6);

        Assert.assertEquals(OrdStatus.PARTIALLY_FILLED, orderBuy6.getOrdStatus());
        Assert.assertEquals(100, orderBuy6.getCumQty());
        Assert.assertEquals(80, orderBuy6.getLeavesQty());
        Assert.assertEquals(ProcessNewOrderSingleResult.PARTIALLY_FILLED, orderMessageBuy3ProcessNewOrderSingleResult);

        Assert.assertEquals(OrdStatus.FILLED, orderSell3.getOrdStatus());
        Assert.assertEquals(100, orderSell3.getCumQty());
        Assert.assertEquals(0, orderSell3.getLeavesQty());


        // send a sell order
        Order orderSell4 = new Order(7, 5.5, Helper.SIDE_SELL, 150);

        ProcessNewOrderSingleResult orderMessageSell4ProcessNewOrderSingleResult = orderMatchingEngine.processNewOrderSingle(orderSell4);
        Assert.assertEquals(ProcessNewOrderSingleResult.FULLY_FILLED, orderMessageSell4ProcessNewOrderSingleResult);
        Assert.assertEquals(OrdStatus.FILLED, orderSell4.getOrdStatus());
        Assert.assertEquals(150, orderSell4.getCumQty());
        Assert.assertEquals(0, orderSell4.getLeavesQty());

    }


    @Test
    public void verifyIfPriceLevelsAreClearedAfterOrdersWereFullyFilled() throws InterruptedException {

        OrderMatchingEngine.getInstance().reset();

        // create mock sell orders in orderbook
        Order orderSell1 = new Order(1, 4, Helper.SIDE_SELL, 100);
        Order orderSell2 = new Order(2, 5.5, Helper.SIDE_SELL, 100);
        Order orderSell3 = new Order(3, 6, Helper.SIDE_SELL, 100);

        OrderMatchingEngine orderMatchingEngine = OrderMatchingEngine.getInstance();
        orderMatchingEngine.processNewOrderSingle(orderSell1);
        orderMatchingEngine.processNewOrderSingle(orderSell2);
        orderMatchingEngine.processNewOrderSingle(orderSell3);

        // send  buy orders
        Order orderBuy1 = new Order(4, 4, Helper.SIDE_BUY, 100);
        Order orderBuy2 = new Order(5, 5.5, Helper.SIDE_BUY, 100);
        Order orderBuy3 = new Order(6, 6, Helper.SIDE_BUY, 100);

        orderMatchingEngine.processNewOrderSingle(orderBuy1);
        orderMatchingEngine.processNewOrderSingle(orderBuy2);
        orderMatchingEngine.processNewOrderSingle(orderBuy3);

        Assert.assertEquals(0, orderMatchingEngine.getBidPriceLevels().size());
        Assert.assertEquals(0, orderMatchingEngine.getAskPriceLevels().size());

        OrderMatchingEngine.getInstance().reset();
    }

    @Test
    public void testSendingCancelRequestForLimitOrder() throws InterruptedException { // Given an order id, remove an Order from the OrderBook (order deletions are expected to occur at approximately 60% of the rate of order additions)

        OrderMatchingEngine.getInstance().reset();

        // create mock sell order in orderbook
        Order orderSell1 = new Order(1, 4, Helper.SIDE_SELL, 100);

        OrderMatchingEngine orderMatchingEngine = OrderMatchingEngine.getInstance();
        orderMatchingEngine.processNewOrderSingle(orderSell1);

        // send a cancel request
        CancelRequest orderCancel1 = new CancelRequest(1);

        ProcessCancelRequestResult processCancelRequestResult = orderMatchingEngine.processCancelRequest(orderCancel1);

        Assert.assertEquals(ProcessCancelRequestResult.CANCELED, processCancelRequestResult);
        Assert.assertEquals(0, orderMatchingEngine.getBidPriceLevels().size());
        Assert.assertEquals(0, orderMatchingEngine.getAskPriceLevels().size());

        OrderMatchingEngine.getInstance().reset();
    }

    @Test
    public void testSendingReplaceRequestForLimitOrder() throws InterruptedException { // Given an order id and a new size, modify an existing order in the book to use the new size (size modifications do not effect time priority)

        OrderMatchingEngine.getInstance().reset();

        // create mock sell orders in orderbook
        Order orderSell1 = new Order(1, 4, Helper.SIDE_SELL, 100);
        Order orderSell2 = new Order(2, 5, Helper.SIDE_SELL, 50);

        OrderMatchingEngine orderMatchingEngine = OrderMatchingEngine.getInstance();
        orderMatchingEngine.processNewOrderSingle(orderSell1);
        orderMatchingEngine.processNewOrderSingle(orderSell2);

        // send replace requests
        ReplaceRequest replaceRequest1 = new ReplaceRequest(1, 200);
        ReplaceRequest replaceRequest2 = new ReplaceRequest(2, 400);

        ProcessReplaceRequestResult processReplaceRequestResult = orderMatchingEngine.processReplaceRequest(replaceRequest1);
        ProcessReplaceRequestResult processReplaceRequestResult2 = orderMatchingEngine.processReplaceRequest(replaceRequest2);

        Assert.assertEquals(ProcessReplaceRequestResult.REPLACED, processReplaceRequestResult);
        Assert.assertEquals(ProcessReplaceRequestResult.REPLACED, processReplaceRequestResult2);
        Assert.assertEquals(2, orderMatchingEngine.getAskPriceLevels().size());
        Assert.assertEquals(200, orderMatchingEngine.getAskPriceLevels().get(orderSell1.getPrice()).get(orderSell1.getOrderId()).getSize());
        Assert.assertEquals(400, orderMatchingEngine.getAskPriceLevels().get(orderSell2.getPrice()).get(orderSell2.getOrderId()).getSize());

        OrderMatchingEngine.getInstance().reset();
    }

    @Test
    public void testSendingReplaceRequestAndThenCancelForLimitOrder() throws InterruptedException {

        OrderMatchingEngine.getInstance().reset();

        // create mock sell order in orderbook
        Order orderSell1 = new Order(1, 4, Helper.SIDE_SELL, 100);
        Order orderSell2 = new Order(2, 5, Helper.SIDE_SELL, 50);

        OrderMatchingEngine orderMatchingEngine = OrderMatchingEngine.getInstance();
        orderMatchingEngine.processNewOrderSingle(orderSell1);
        orderMatchingEngine.processNewOrderSingle(orderSell2);

        // send a cancel request
        ReplaceRequest replaceRequest1 = new ReplaceRequest(1, 200);
        ReplaceRequest replaceRequest2 = new ReplaceRequest(2, 400);

        ProcessReplaceRequestResult processReplaceRequestResult = orderMatchingEngine.processReplaceRequest(replaceRequest1);
        ProcessReplaceRequestResult processReplaceRequestResult2 = orderMatchingEngine.processReplaceRequest(replaceRequest2);

        Assert.assertEquals(ProcessReplaceRequestResult.REPLACED, processReplaceRequestResult);
        Assert.assertEquals(ProcessReplaceRequestResult.REPLACED, processReplaceRequestResult2);
        Assert.assertEquals(2, orderMatchingEngine.getAskPriceLevels().size());
        Assert.assertEquals(200, orderMatchingEngine.getAskPriceLevels().get(orderSell1.getPrice()).get(orderSell1.getOrderId()).getSize());
        Assert.assertEquals(400, orderMatchingEngine.getAskPriceLevels().get(orderSell2.getPrice()).get(orderSell2.getOrderId()).getSize());

        CancelRequest cancelRequest1 = new CancelRequest(1);
        ProcessCancelRequestResult processCancelRequestResult1 = orderMatchingEngine.processCancelRequest(cancelRequest1);
        Assert.assertEquals(ProcessCancelRequestResult.CANCELED, processCancelRequestResult1);
        Assert.assertEquals(1, orderMatchingEngine.getAskPriceLevels().size());


        OrderMatchingEngine.getInstance().reset();
    }


    @Test
    public void testGetPriceForLevelAndSideForBuyOrders() throws InterruptedException { // Given a side and a level (an integer value >0) return the price for that level (where level 1 represents the best price for a given side). For example, given side=B and level=2 return the second best bid price

        OrderMatchingEngine.getInstance().reset();


        // send multiple buy orders
        Order orderBuy1 = new Order(1, 4, Helper.SIDE_BUY, 100);
        Order orderBuy2 = new Order(2, 5.5, Helper.SIDE_BUY, 100);
        Order orderBuy3 = new Order(3, 6, Helper.SIDE_BUY, 100);
        Order orderBuy4 = new Order(4, 7, Helper.SIDE_BUY, 100);
        Order orderBuy5 = new Order(5, 8, Helper.SIDE_BUY, 100);

        OrderMatchingEngine orderMatchingEngine = OrderMatchingEngine.getInstance();
        orderMatchingEngine.processNewOrderSingle(orderBuy1);
        orderMatchingEngine.processNewOrderSingle(orderBuy2);
        orderMatchingEngine.processNewOrderSingle(orderBuy3);
        orderMatchingEngine.processNewOrderSingle(orderBuy4);
        orderMatchingEngine.processNewOrderSingle(orderBuy5);

        Assert.assertEquals(7, orderMatchingEngine.getPriceForLevelAndSide(2, Helper.SIDE_BUY), 0.0001);

        // send a sell order
        Order orderSell1 = new Order(6, 7, Helper.SIDE_SELL, 100);

        ProcessNewOrderSingleResult orderMessageSell1ProcessNewOrderSingleResult = orderMatchingEngine.processNewOrderSingle(orderSell1);
        Assert.assertEquals(ProcessNewOrderSingleResult.FULLY_FILLED, orderMessageSell1ProcessNewOrderSingleResult);


        Assert.assertEquals(6, orderMatchingEngine.getPriceForLevelAndSide(2, Helper.SIDE_BUY), 0.0001);

        OrderMatchingEngine.getInstance().reset();

    }


    @Test
    public void testGetPriceForLevelAndSideForSellOrders() throws InterruptedException { // Given a side and a level (an integer value >0) return the price for that level (where level 1 represents the best price for a given side). For example, given side=B and level=2 return the second best bid price

        OrderMatchingEngine.getInstance().reset();


        // send multiple sell orders
        Order orderSell1 = new Order(1, 8, Helper.SIDE_SELL, 100);
        Order orderSell2 = new Order(2, 7, Helper.SIDE_SELL, 100);
        Order orderSell3 = new Order(3, 6, Helper.SIDE_SELL, 100);
        Order orderSell4 = new Order(4, 5.5, Helper.SIDE_SELL, 100);
        Order orderSell5 = new Order(5, 4, Helper.SIDE_SELL, 100);

        OrderMatchingEngine orderMatchingEngine = OrderMatchingEngine.getInstance();
        orderMatchingEngine.processNewOrderSingle(orderSell1);
        orderMatchingEngine.processNewOrderSingle(orderSell2);
        orderMatchingEngine.processNewOrderSingle(orderSell3);
        orderMatchingEngine.processNewOrderSingle(orderSell4);
        orderMatchingEngine.processNewOrderSingle(orderSell5);

        Assert.assertEquals(5.5, orderMatchingEngine.getPriceForLevelAndSide(2, Helper.SIDE_SELL), 0.0001);

        // send a buy order
        Order orderBuy1 = new Order(6, 7, Helper.SIDE_BUY, 100);

        ProcessNewOrderSingleResult orderMessageSell1ProcessNewOrderSingleResult = orderMatchingEngine.processNewOrderSingle(orderBuy1);
        Assert.assertEquals(ProcessNewOrderSingleResult.FULLY_FILLED, orderMessageSell1ProcessNewOrderSingleResult);

        Assert.assertEquals(6, orderMatchingEngine.getPriceForLevelAndSide(2, Helper.SIDE_SELL), 0.0001);

        OrderMatchingEngine.getInstance().reset();

    }


    @Test
    public void testTotalSizeAvailableForThatLevel() throws InterruptedException { // Given a side and a level return the total size available for that level

        OrderMatchingEngine.getInstance().reset();


        // send multiple sell orders
        Order orderSell1 = new Order(1, 6, Helper.SIDE_SELL, 100);
        Order orderSell2 = new Order(2, 5, Helper.SIDE_SELL, 100);
        Order orderSell3 = new Order(3, 4, Helper.SIDE_SELL, 100);
        Order orderSell4 = new Order(4, 4, Helper.SIDE_SELL, 100);
        Order orderSell5 = new Order(5, 4, Helper.SIDE_SELL, 100);

        OrderMatchingEngine orderMatchingEngine = OrderMatchingEngine.getInstance();
        orderMatchingEngine.processNewOrderSingle(orderSell1);
        orderMatchingEngine.processNewOrderSingle(orderSell2);
        orderMatchingEngine.processNewOrderSingle(orderSell3);
        orderMatchingEngine.processNewOrderSingle(orderSell4);
        orderMatchingEngine.processNewOrderSingle(orderSell5);

        Assert.assertEquals(3, orderMatchingEngine.getTotalSizeForSpecificLevel(1, Helper.SIDE_SELL));
        Assert.assertEquals(1, orderMatchingEngine.getTotalSizeForSpecificLevel(2, Helper.SIDE_SELL));

        Order orderBuy1 = new Order(6, 4, Helper.SIDE_BUY, 100);

        ProcessNewOrderSingleResult orderMessageSell1ProcessNewOrderSingleResult = orderMatchingEngine.processNewOrderSingle(orderBuy1);
        Assert.assertEquals(ProcessNewOrderSingleResult.FULLY_FILLED, orderMessageSell1ProcessNewOrderSingleResult);

        Assert.assertEquals(2, orderMatchingEngine.getTotalSizeForSpecificLevel(1, Helper.SIDE_SELL));

        OrderMatchingEngine.getInstance().reset();
    }

    @Test
    public void testGetAllOrdersFromSpecificSide() throws InterruptedException { // Given a side return all the orders from that side of the book, in level- and time-order

        OrderMatchingEngine.getInstance().reset();

        // send multiple sell orders
        Order orderSell1 = new Order(1, 6, Helper.SIDE_SELL, 100);
        Order orderSell2 = new Order(2, 5, Helper.SIDE_SELL, 100);
        Order orderSell3 = new Order(3, 4, Helper.SIDE_SELL, 100);
        Order orderSell4 = new Order(4, 4, Helper.SIDE_SELL, 100);
        Order orderSell5 = new Order(5, 4, Helper.SIDE_SELL, 100);

        OrderMatchingEngine orderMatchingEngine = OrderMatchingEngine.getInstance();
        orderMatchingEngine.processNewOrderSingle(orderSell1);
        orderMatchingEngine.processNewOrderSingle(orderSell2);
        orderMatchingEngine.processNewOrderSingle(orderSell3);
        orderMatchingEngine.processNewOrderSingle(orderSell4);
        orderMatchingEngine.processNewOrderSingle(orderSell5);

        List<Order> listOfSellOrders = orderMatchingEngine.getAllOrdersFromSpecificSide(Helper.SIDE_SELL);
        Assert.assertEquals(5, listOfSellOrders.size());
        Assert.assertEquals(orderSell3, listOfSellOrders.get(0));
        Assert.assertEquals(orderSell4, listOfSellOrders.get(1));
        Assert.assertEquals(orderSell5, listOfSellOrders.get(2));
        Assert.assertEquals(orderSell2, listOfSellOrders.get(3));
        Assert.assertEquals(orderSell1, listOfSellOrders.get(4));

        Order orderBuy1 = new Order(6, 4, Helper.SIDE_BUY, 100);

        ProcessNewOrderSingleResult orderMessageSell1ProcessNewOrderSingleResult = orderMatchingEngine.processNewOrderSingle(orderBuy1);
        Assert.assertEquals(ProcessNewOrderSingleResult.FULLY_FILLED, orderMessageSell1ProcessNewOrderSingleResult);

        List<Order> listOfSellOrdersNew = orderMatchingEngine.getAllOrdersFromSpecificSide(Helper.SIDE_SELL);
        Assert.assertEquals(4, listOfSellOrdersNew.size());
        Assert.assertEquals(orderSell4, listOfSellOrdersNew.get(0));
        Assert.assertEquals(orderSell5, listOfSellOrdersNew.get(1));
        Assert.assertEquals(orderSell2, listOfSellOrdersNew.get(2));
        Assert.assertEquals(orderSell1, listOfSellOrdersNew.get(3));

        OrderMatchingEngine.getInstance().reset();
    }

    @Test
    public void testSendingDuplicatedOrderIds() throws InterruptedException {
        OrderMatchingEngine.getInstance().reset();

        // create mock orders in orderbook
        Order orderSell1 = new Order(1, 4, Helper.SIDE_SELL, 100);
        Order orderSell2 = new Order(1, 5.5, Helper.SIDE_SELL, 100);
        Order orderSell3 = new Order(1, 5.5, Helper.SIDE_SELL, 100);

        OrderMatchingEngine orderMatchingEngine = OrderMatchingEngine.getInstance();
        ProcessNewOrderSingleResult orderMessageSell1Result = orderMatchingEngine.processNewOrderSingle(orderSell1);
        ProcessNewOrderSingleResult orderMessageSell2Result = orderMatchingEngine.processNewOrderSingle(orderSell2);
        ProcessNewOrderSingleResult orderMessageSell3Result = orderMatchingEngine.processNewOrderSingle(orderSell3);

        Assert.assertEquals(ProcessNewOrderSingleResult.NEW, orderMessageSell1Result);
        Assert.assertEquals(ProcessNewOrderSingleResult.FAIL_DUPLICATE_ORDERID, orderMessageSell2Result);
        Assert.assertEquals(ProcessNewOrderSingleResult.FAIL_DUPLICATE_ORDERID, orderMessageSell3Result);

        OrderMatchingEngine.getInstance().reset();
    }

}
