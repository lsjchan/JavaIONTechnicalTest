This assignment uses Java 8 maven build.

## Part A: Please view JavaIONTechnicalTest's  '**OrderMatchingEngineTest.java**' for unit test cases:

- Given an Order, add it to the OrderBook (order additions are expected to occur extremely frequently) ---- please view **testSendingSellLimitOrderInABidDominatedBook()** and **testSendingBuyLimitOrderInASellDominatedBook()**
- Given an order id, remove an Order from the OrderBook (order deletions are expected to occur at approximately 60% of the rate of order additions) ---- please view **testSendingCancelRequestForLimitOrder()**
- Given an order id and a new size, modify an existing order in the book to use the new size (size modifications do not effect time priority) ---- please view **testSendingReplaceRequestForLimitOrder()**
- Given a side and a level (an integer value >0) return the price for that level (where level 1 represents the best price for a given side). For example, given side=B and level=2 return the second best bid price ---- please view **testGetPriceForLevelAndSideForBuyOrders()** and **testGetPriceForLevelAndSideForSellOrders()**
- Given a side and a level return the total size available for that level ---- please view **testTotalSizeAvailableForThatLevel()**
- Given a side return all the orders from that side of the book, in level- and time-order ---- please view **testGetAllOrdersFromSpecificSide()**

## Part B (suggestions of potential improvements):
- implement market order,
- add symbol to reflect realworld order book matching engine,
- implement symbol-level lock to reduce lock contention,
- add more attributes/fields to Order class like "customTags" and "MsgType"
