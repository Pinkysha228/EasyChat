package me.pinkysha.easychat.chat;

public enum ChatChannel {
    LOCAL("local"), GLOBAL("global"), ADMIN("admin");
    private final String id;
    private String symbol;
    private String permission;
    private String format;
    private double radius;
    private boolean enabled;
    ChatChannel(String id) { this.id = id; }
    public String id() { return id; }
    public String symbol() { return symbol; }
    public String permission() { return permission; }
    public String format() { return format; }
    public double radius() { return radius; }
    public boolean enabled() { return enabled; }
    public void configure(String symbol, String permission, String format, double radius, boolean enabled) { this.symbol=symbol;this.permission=permission;this.format=format;this.radius=radius;this.enabled=enabled; }
}
