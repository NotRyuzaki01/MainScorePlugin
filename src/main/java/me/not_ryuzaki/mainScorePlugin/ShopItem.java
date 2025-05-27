package me.not_ryuzaki.mainScorePlugin;

public class ShopItem {
    private final int price;
    private final int amount;

    public ShopItem(int price, int amount) {
        this.price = price;
        this.amount = amount;
    }

    public int getPrice() {
        return price;
    }

    public int getAmount() {
        return amount;
    }
}
