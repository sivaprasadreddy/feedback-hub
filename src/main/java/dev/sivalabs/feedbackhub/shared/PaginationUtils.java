package dev.sivalabs.feedbackhub.shared;

public class PaginationUtils {
    private PaginationUtils(){}

    public static int parsePage(String page) {
        try {
            var pageNo = Integer.parseInt(page);
            if (pageNo < 1) {
                throw new BadRequestException("Page number must be at least 1");
            }
            return pageNo;
        } catch (NumberFormatException e) {
            throw new BadRequestException("Page number must be a positive integer");
        }
    }
}
