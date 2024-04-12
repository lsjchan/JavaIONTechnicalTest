import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class OrderMatchingEngine {

    private Lock lock;
    private static OrderMatchingEngine orderMatchingEngine;
    private TreeMap<Double, LinkedHashMap<Long, Order>> bidPriceLevels; // key = price (BigDecimal), value = LinkedHashMap storing orders [ key = orderId(long) and value = Order(s)]
    private TreeMap<Double, LinkedHashMap<Long, Order>> askPriceLevels; // key = price (BigDecimal), value = LinkedHashMap storing orders [ key = orderId(long) and value = Order(s)]

    private double bestBid, bestAsk;


    private Map<Long, Order> ordersInOrderBook; // key = orderid, value = OrderMessage object

    private OrderMatchingEngine() {
        ordersInOrderBook = new HashMap<>();

        bidPriceLevels = new TreeMap<>(Collections.reverseOrder());
        askPriceLevels = new TreeMap<>();

        lock = new ReentrantLock();
    }

    public static OrderMatchingEngine getInstance(){
        if (null == orderMatchingEngine){
            synchronized (OrderMatchingEngine.class){
                if (null == orderMatchingEngine){
                    synchronized (OrderMatchingEngine.class){
                        orderMatchingEngine = new OrderMatchingEngine();
                    }
                }
            }
        }
        return orderMatchingEngine;
    }



    public ProcessNewOrderSingleResult processNewOrderSingle(Order incomingOrder) {

        // fat finger check
        if (incomingOrder.getSize() < 0 || incomingOrder.getPrice() <= 0 || 0 == incomingOrder.getOrderId() ){
            return ProcessNewOrderSingleResult.INVALID_INPUT;
        }

        try {
            lock.lock();
            //  check if orderId already exist in "private Map<Long, Order> ordersInOrderBook;"
            if (ordersInOrderBook.containsKey(incomingOrder.getOrderId())) {

                return ProcessNewOrderSingleResult.FAIL_DUPLICATE_ORDERID;
            }
            ordersInOrderBook.put(incomingOrder.getOrderId(), incomingOrder);


            if (Helper.SIDE_SELL == incomingOrder.getSide()) {

                if ((bidPriceLevels.size() > 0 && bestBid != 0.0 && incomingOrder.getPrice() <= bestBid)) {
                    return matchOrders(incomingOrder, bidPriceLevels); // See if it can match any existing orders
                } else {

                    return processCumQtyGreaterOrEqualTo0(incomingOrder, askPriceLevels);
                }

            } else if (Helper.SIDE_BUY == incomingOrder.getSide()) {

                if ((askPriceLevels.size() > 0 && bestAsk != 0.0 && incomingOrder.getPrice() >= bestAsk)) {
                    return matchOrders(incomingOrder, askPriceLevels); // See if it can match any existing orders
                } else {

                    return processCumQtyGreaterOrEqualTo0(incomingOrder, bidPriceLevels);
                }
            }

            return ProcessNewOrderSingleResult.NEW;
        } finally {

            lock.unlock();
        }
    }

    private ProcessNewOrderSingleResult matchOrders(final Order incomingOrder, final Map<Double, LinkedHashMap<Long, Order>> priceLevelsInOrderBook) {

        long inputOrderLeavesQty = incomingOrder.getLeavesQty();
        long inputOrderCumQty = incomingOrder.getCumQty();
        final boolean incomingOrderIsBuy = incomingOrder.getSide() == Helper.SIDE_BUY;
        final double incomingOrderLimitPrice = incomingOrder.getPrice();

        Iterator<Map.Entry<Double, LinkedHashMap<Long, Order>>> priceLevelsEntryIterator = priceLevelsInOrderBook.entrySet().iterator();
        while (priceLevelsEntryIterator.hasNext()) {
            Map.Entry<Double, LinkedHashMap<Long, Order>> priceLevelsEntry = priceLevelsEntryIterator.next();

            if (((incomingOrderIsBuy && incomingOrderLimitPrice < priceLevelsEntry.getKey()) || (!incomingOrderIsBuy && incomingOrderLimitPrice > priceLevelsEntry.getKey()))) {
                break;
            }

            Iterator<Map.Entry<Long, Order>> orderInOrderBookIterator = priceLevelsEntry.getValue().entrySet().iterator();
            while (orderInOrderBookIterator.hasNext()) {

                Map.Entry<Long, Order> orderInOrderBookEntry = orderInOrderBookIterator.next();
                Order orderInOrderBook = orderInOrderBookEntry.getValue();

                long leavesQtyOfOrderMsgInOrderBook = orderInOrderBook.getLeavesQty();


                if (inputOrderLeavesQty > leavesQtyOfOrderMsgInOrderBook) { // Scenario 1: client order partial fill, and order in order book full fill


                    inputOrderLeavesQty -= leavesQtyOfOrderMsgInOrderBook;
                    inputOrderCumQty += leavesQtyOfOrderMsgInOrderBook;

                    incomingOrder.setLeavesQty(inputOrderLeavesQty);
                    incomingOrder.setCumQty(inputOrderCumQty);
                    incomingOrder.setOrdStatus(OrdStatus.PARTIALLY_FILLED);
                    incomingOrder.setLastPx(priceLevelsEntry.getKey());

                    setOrderInOrderBookWhenIncomingOrderLeavesQtyGreaterOrEqualsThanOrderBookLeaveQty(incomingOrder, orderInOrderBook, priceLevelsEntry);
                    orderInOrderBookIterator.remove();

                    if (0 == priceLevelsEntry.getValue().size()) {
                        priceLevelsEntryIterator.remove(); // remove price level if no longer contain order

                        if (incomingOrderIsBuy) { // call to update best bid/ask once price level is updated
                            updateBestAskFromOrderBook();
                        } else {
                            updateBestBidFromOrderBook();
                        }
                    }


                } else if (inputOrderLeavesQty == leavesQtyOfOrderMsgInOrderBook) { // Scenario 2: incoming order full fill, "existing order in order book" full fill

                    inputOrderLeavesQty = 0;
                    inputOrderCumQty += leavesQtyOfOrderMsgInOrderBook;

                    incomingOrder.setLeavesQty(inputOrderLeavesQty);
                    incomingOrder.setCumQty(inputOrderCumQty);
                    incomingOrder.setOrdStatus(OrdStatus.FILLED);
                    incomingOrder.setLastPx(priceLevelsEntry.getKey());

                    setOrderInOrderBookWhenIncomingOrderLeavesQtyGreaterOrEqualsThanOrderBookLeaveQty(incomingOrder, orderInOrderBook, priceLevelsEntry);

                    orderInOrderBookIterator.remove();

                    if (0 == priceLevelsEntry.getValue().size()) {
                        priceLevelsEntryIterator.remove(); // remove price level if no longer contain order

                        if (incomingOrderIsBuy) { // call to update best bid/ask once price level is updated
                            updateBestAskFromOrderBook();
                        } else {
                            updateBestBidFromOrderBook();
                        }
                    }


                    return ProcessNewOrderSingleResult.FULLY_FILLED;


                } else { // Scenario 3: incoming order full fill, "existing order in order book" partial fill

                    long newOrderBookMessageLeavesQty = orderInOrderBook.getLeavesQty() - inputOrderLeavesQty;
                    long commonExecQty = inputOrderLeavesQty;

                    inputOrderLeavesQty = 0;
                    inputOrderCumQty = incomingOrder.getSize();

                    incomingOrder.setLeavesQty(inputOrderLeavesQty);
                    incomingOrder.setCumQty(inputOrderCumQty);
                    incomingOrder.setOrdStatus(OrdStatus.FILLED);
                    incomingOrder.setLastPx(priceLevelsEntry.getKey());

                    orderInOrderBook.setLeavesQty(newOrderBookMessageLeavesQty);
                    orderInOrderBook.setCumQty(orderInOrderBook.getCumQty() + commonExecQty);
                    orderInOrderBook.setOrdStatus(OrdStatus.PARTIALLY_FILLED);
                    orderInOrderBook.setLastPx(priceLevelsEntry.getKey());

                    return ProcessNewOrderSingleResult.FULLY_FILLED;

                }

            }
        }

        return Helper.SIDE_SELL == incomingOrder.getSide() ? processCumQtyGreaterOrEqualTo0(incomingOrder, askPriceLevels) :  processCumQtyGreaterOrEqualTo0(incomingOrder, bidPriceLevels);
    }

    private void setOrderInOrderBookWhenIncomingOrderLeavesQtyGreaterOrEqualsThanOrderBookLeaveQty(Order orderInput, Order orderMessageInOrderBook, Map.Entry<Double, LinkedHashMap<Long, Order>> priceLevelsEntry){
        orderMessageInOrderBook.setLeavesQty(0);
        orderMessageInOrderBook.setCumQty(orderMessageInOrderBook.getSize());
        orderMessageInOrderBook.setOrdStatus(OrdStatus.FILLED);
        orderMessageInOrderBook.setLastPx(priceLevelsEntry.getKey());
        ordersInOrderBook.remove(orderMessageInOrderBook.getOrderId());
    }

    private ProcessNewOrderSingleResult processCumQtyGreaterOrEqualTo0(Order incomingOrder, Map<Double, LinkedHashMap<Long, Order>> bidOrAskPriceLevels){
        if (incomingOrder.getCumQty() == 0) {
            incomingOrder.setOrdStatus(OrdStatus.NEW);
        }

        bidOrAskPriceLevels.computeIfAbsent(incomingOrder.getPrice(), x -> new LinkedHashMap<>()).put(incomingOrder.getOrderId(), incomingOrder);


        if (Helper.SIDE_BUY == incomingOrder.getSide()){ // call to update best bid/ask

            updateBestBidFromOrderBook();
        } else if (Helper.SIDE_SELL == incomingOrder.getSide()){

            updateBestAskFromOrderBook();
        }

        if (incomingOrder.getCumQty() > 0) {

            return ProcessNewOrderSingleResult.PARTIALLY_FILLED;
        } else {

            return ProcessNewOrderSingleResult.NEW;
        }
    }


    public ProcessCancelRequestResult processCancelRequest(CancelRequest incomingCancelRequest) throws InterruptedException {

        try {
            lock.lock();

            if (!ordersInOrderBook.containsKey(incomingCancelRequest.getOrigOrderId())) {

                return ProcessCancelRequestResult.CANCEL_REJECT__CANNOT_FIND_IN_ORDERBOOK;
            }

            double priceOfExistingOrder = ordersInOrderBook.get(incomingCancelRequest.getOrigOrderId()).getPrice();
            char side = ordersInOrderBook.get(incomingCancelRequest.getOrigOrderId()).getSide();

            if (priceOfExistingOrder > 0 && ( Helper.SIDE_BUY == side || Helper.SIDE_SELL == side)) { /* if can find existing order using given price, then we just need to go to that price level and remove that order */
                if (Helper.SIDE_BUY == side) {
                    if (null != bidPriceLevels.get(priceOfExistingOrder) || bidPriceLevels.get(priceOfExistingOrder).size() != 0) {

                        if (removeOrder(incomingCancelRequest, bidPriceLevels, priceOfExistingOrder, bidPriceLevels.get(priceOfExistingOrder).get(incomingCancelRequest.getOrigOrderId())))
                            return ProcessCancelRequestResult.CANCELED;
                    }
                } else if (Helper.SIDE_SELL == side) {
                    if (null != askPriceLevels.get(priceOfExistingOrder) || askPriceLevels.get(priceOfExistingOrder).size() != 0) {

                        if (removeOrder(incomingCancelRequest, askPriceLevels, priceOfExistingOrder, askPriceLevels.get(priceOfExistingOrder).get(incomingCancelRequest.getOrigOrderId())))
                            return ProcessCancelRequestResult.CANCELED;
                    }
                } else {
                    return ProcessCancelRequestResult.CANCEL_REJECT__CANNOT_FIND_SIDE;
                }
            }



            /* if cannot find existing order using given price, then we need to scan each price level in both bidPriceLevel and askPriceLevel to find the order */
            if (ScanInBothSidesOrderBookToCancel(incomingCancelRequest, priceOfExistingOrder, bidPriceLevels))
                return ProcessCancelRequestResult.CANCELED;

            if (ScanInBothSidesOrderBookToCancel(incomingCancelRequest, priceOfExistingOrder, askPriceLevels))
                return ProcessCancelRequestResult.CANCELED;

            return ProcessCancelRequestResult.CANCEL_REJECT__CANNOT_FIND_IN_ORDERBOOK;
//        }
        } finally {
            lock.unlock();
        }
    }


    private boolean removeOrder( CancelRequest incomingCancelRequest, Map<Double, LinkedHashMap<Long, Order>> priceLevels, double priceOfExistingOrder, Order existingOrderInOrderBook) throws InterruptedException {
        if (null != existingOrderInOrderBook) {
            existingOrderInOrderBook = priceLevels.get(priceOfExistingOrder).remove(incomingCancelRequest.getOrigOrderId()); // remove order from price level

            if (0 == priceLevels.get(priceOfExistingOrder).size()) {
                priceLevels.remove(priceOfExistingOrder); // remove price level if no more orders in the price level

                if (Helper.SIDE_BUY == existingOrderInOrderBook.getSide()){ // call to update best bid/ask once price level is updated

                    updateBestAskFromOrderBook();
                } else if (Helper.SIDE_SELL == existingOrderInOrderBook.getSide()){

                    updateBestBidFromOrderBook();
                }
            }

            existingOrderInOrderBook.setOrdStatus(OrdStatus.CANCELED);
            ordersInOrderBook.remove(incomingCancelRequest.getOrigOrderId());

            if (existingOrderInOrderBook != null){
                return true;
            }
        }
        return false;
    }

    private boolean ScanInBothSidesOrderBookToCancel(CancelRequest incomingCancelRequest, double priceOfExistingOrder, TreeMap<Double, LinkedHashMap<Long, Order>> bidPriceLevels) throws InterruptedException {
        Iterator<Map.Entry<Double, LinkedHashMap<Long, Order>>> bidPriceLevelIterator = bidPriceLevels.entrySet().iterator();
        while (bidPriceLevelIterator.hasNext()) {

            LinkedHashMap<Long, Order> bidPriceLevel = bidPriceLevelIterator.next().getValue();
            if (null != bidPriceLevel.get(incomingCancelRequest.getOrigOrderId())) {
                if (removeOrder(incomingCancelRequest, bidPriceLevels, priceOfExistingOrder, bidPriceLevel.get(incomingCancelRequest.getOrigOrderId())))
                    return true;
            }
        }
        return false;
    }

    public ProcessReplaceRequestResult processReplaceRequest(ReplaceRequest incomingReplaceRequest) throws InterruptedException {

        // fat finger check
        if (incomingReplaceRequest.getNewSize() < 0 ) {
            return ProcessReplaceRequestResult.REPLACE_REJECT__INVALID_INPUT;
        }

        try {
            lock.lock();

            if (!ordersInOrderBook.containsKey(incomingReplaceRequest.getOrigOrderId())) {

                return ProcessReplaceRequestResult.REPLACE_REJECT__CANNOT_FIND_IN_ORDERBOOK;
            }


            double priceOfExistingOrder = ordersInOrderBook.get(incomingReplaceRequest.getOrigOrderId()).getPrice();
            char sideOfExistingOrder = ordersInOrderBook.get(incomingReplaceRequest.getOrigOrderId()).getSide();

            if (priceOfExistingOrder > 0 && ( Helper.SIDE_BUY == sideOfExistingOrder || Helper.SIDE_SELL == sideOfExistingOrder)) { /* if can find existing order using given price, then we just need to go to that price level and remove that order */
                if (Helper.SIDE_BUY == sideOfExistingOrder) {
                    if (null != bidPriceLevels.get(priceOfExistingOrder) || bidPriceLevels.get(priceOfExistingOrder).size() != 0) {

                        if (replaceOrderWithNewSize(incomingReplaceRequest, bidPriceLevels, priceOfExistingOrder, bidPriceLevels.get(priceOfExistingOrder).get(incomingReplaceRequest.getOrigOrderId())))
                            return ProcessReplaceRequestResult.REPLACED;
                    }
                }
                else if (Helper.SIDE_SELL == sideOfExistingOrder) {
                    if (null != askPriceLevels.get(priceOfExistingOrder) || askPriceLevels.get(priceOfExistingOrder).size() != 0) {

                        if (replaceOrderWithNewSize(incomingReplaceRequest, askPriceLevels, priceOfExistingOrder, askPriceLevels.get(priceOfExistingOrder).get(incomingReplaceRequest.getOrigOrderId())))
                            return ProcessReplaceRequestResult.REPLACED;
                    }
                }
            }


            /* if cannot find existing order using given price, then we need to scan each price level in both bidPriceLevel and askPriceLevel to find the order */
            if (ScanInBothSidesOrderBookToReplace(incomingReplaceRequest, priceOfExistingOrder, bidPriceLevels))
                return ProcessReplaceRequestResult.REPLACED;

            if (ScanInBothSidesOrderBookToReplace(incomingReplaceRequest, priceOfExistingOrder, askPriceLevels))
                return ProcessReplaceRequestResult.REPLACED;

            return ProcessReplaceRequestResult.REPLACE_REJECT__CANNOT_FIND_IN_ORDERBOOK;
    //        }
        } finally {
            lock.unlock();
        }
    }

    private boolean replaceOrderWithNewSize( ReplaceRequest incomingReplaceRequest, Map<Double, LinkedHashMap<Long, Order>> priceLevels, double priceOfExistingOrder, Order existingOrderInOrderBook) {
        if (null != existingOrderInOrderBook) {
            existingOrderInOrderBook = priceLevels.get(priceOfExistingOrder).get(incomingReplaceRequest.getOrigOrderId()); // remove order from price level
            existingOrderInOrderBook.setSize(incomingReplaceRequest.getNewSize());

            existingOrderInOrderBook.setOrdStatus(OrdStatus.REPLACED);

            if (existingOrderInOrderBook != null){
                return true;
            }
        }
        return false;
    }


    private boolean ScanInBothSidesOrderBookToReplace(ReplaceRequest incomingReplaceRequest, double priceOfExistingOrder, TreeMap<Double, LinkedHashMap<Long, Order>> bidPriceLevels) {
        Iterator<Map.Entry<Double, LinkedHashMap<Long, Order>>> bidPriceLevelIterator = bidPriceLevels.entrySet().iterator();
        while (bidPriceLevelIterator.hasNext()) {

            LinkedHashMap<Long, Order> bidPriceLevel = bidPriceLevelIterator.next().getValue();
            if (null != bidPriceLevel.get(incomingReplaceRequest.getOrigOrderId())) {
                if (replaceOrderWithNewSize(incomingReplaceRequest, bidPriceLevels, priceOfExistingOrder, bidPriceLevel.get(incomingReplaceRequest.getOrigOrderId())))
                    return true;
            }
        }
        return false;
    }

    public double getPriceForLevelAndSide(int level, char side){
        try{
            lock.lock();

            if (Helper.SIDE_BUY == side && level <= bidPriceLevels.size() - 1 ){
                return bidPriceLevels.keySet().toArray(new Double[0])[--level]; // if user inputs 1 trying to get 1st element, then since arrays index starts from 0, array will return the 0th element, hence '--level' is used

            } else if (Helper.SIDE_SELL == side && level <= askPriceLevels.size() - 1){
                return askPriceLevels.keySet().toArray(new Double[0])[--level]; // if user inputs 1 trying to get 1st element, then since arrays index starts from 0, array will return the 0th element, hence '--level' is used
            }
            return 0.0;
        } finally {
            lock.unlock();
        }
    }

    public int getTotalSizeForSpecificLevel(int level, char side){
        try{
            lock.lock();
            if (Helper.SIDE_BUY == side){
                return bidPriceLevels.values().toArray(new LinkedHashMap[0])[--level].size(); // if user inputs 1 trying to get 1st element, then since arrays index starts from 0, array will return the 0th element, hence '--level' is used
            } else if (Helper.SIDE_SELL == side){
                return askPriceLevels.values().toArray(new LinkedHashMap[0])[--level].size(); // if user inputs 1 trying to get 1st element, then since arrays index starts from 0, array will return the 0th element, hence '--level' is used
            }
            return 0;
        } finally {
            lock.unlock();
        }
    }


    public List<Order> getAllOrdersFromSpecificSide(char side){
        try{
            lock.lock();
            if (Helper.SIDE_BUY == side){
                List<Order> listOfAllBidOrders = new ArrayList();
                bidPriceLevels.values().stream().forEach(priceLevel -> listOfAllBidOrders.addAll(priceLevel.values()));
                return listOfAllBidOrders;
            } else if (Helper.SIDE_SELL == side){
                List<Order> listOfAllAskOrders = new ArrayList();
                askPriceLevels.values().stream().forEach(priceLevel -> listOfAllAskOrders.addAll(priceLevel.values()));
                return listOfAllAskOrders;
            }
            return null;
        } finally {
            lock.unlock();
        }
    }


    void reset(){
        this.ordersInOrderBook.clear();
        this.bidPriceLevels.clear();
        this.askPriceLevels.clear();
    }

    public TreeMap<Double, LinkedHashMap<Long, Order>> getBidPriceLevels() {
        return bidPriceLevels;
    }

    public TreeMap<Double, LinkedHashMap<Long, Order>> getAskPriceLevels() {
        return askPriceLevels;
    }

    public void updateBestBidFromOrderBook(){
        if (getBidPriceLevels().size() > 0)
            bestBid = getBidPriceLevels().firstKey();
    }

    public void updateBestAskFromOrderBook(){
        if (getAskPriceLevels().size() > 0)
            bestAsk = getAskPriceLevels().firstKey();
    }


}
