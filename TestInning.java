public class TestInning {
    public static void main(String[] args) {
        System.out.println(parseInning("3 1/3"));
        System.out.println(parseInning("3 2/3"));
        System.out.println(parseInning("5"));
        System.out.println(parseInning("1/3"));
        // Sometimes KBO data uses 3.1 for 3 1/3 and 3.2 for 3 2/3
        System.out.println(parseInning("3.1"));
        System.out.println(parseInning("3.2"));
    }

    private static long parseInning(String inningStr) {
        if (inningStr == null || inningStr.isEmpty()) return 0;
        inningStr = inningStr.trim();
        long totalOuts = 0;

        // Handle decimal formats like "3.1" or "3.2" (1 out or 2 outs)
        if (inningStr.contains(".")) {
            String[] parts = inningStr.split("\\.");
            try {
                totalOuts += Long.parseLong(parts[0]) * 3;
                totalOuts += Long.parseLong(parts[1]);
                return totalOuts;
            } catch (Exception ignore) {}
        }

        String[] parts = inningStr.split(" ");
        for (String part : parts) {
            if (part.contains("/")) {
                String[] frac = part.split("/");
                try {
                    totalOuts += Long.parseLong(frac[0]);
                } catch (Exception ignore) {}
            } else {
                try {
                    String num = part.replaceAll("[^0-9]", "");
                    if (!num.isEmpty()) {
                        totalOuts += Long.parseLong(num) * 3;
                    }
                } catch (Exception ignore) {}
            }
        }
        return totalOuts;
    }
}
