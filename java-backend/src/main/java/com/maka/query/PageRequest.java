package com.maka.query;

/**
 * 分页实体
 */
public class PageRequest {
    /**
     * 总数量
     */
    private int totalNum;
    /**
     * 总页数
     */
    private int pageTotalNum;
    /**
     * 每一页的容量
     */
    private int pageSize;
    /**
     * 当前是第几页
     */
    private int currentPage;

    private Object data;


    @Override
    public String toString() {
        return "PageRequest{" +
                "totalNum=" + totalNum +
                ", pageTotalNum=" + pageTotalNum +
                ", pageSize=" + pageSize +
                ", currentPage=" + currentPage +
                ", data=" + data +
                '}';
    }

    public void setTotalNum(int totalNum) {
        this.totalNum = totalNum;
    }

    public void setPageTotalNum(int pageTotalNum) {
        this.pageTotalNum = pageTotalNum;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public Object getData() {
        return data;
    }

    public int getTotalNum() {
        return totalNum;
    }

    public int getPageTotalNum() {
        return pageTotalNum;
    }

    public int getPageSize() {
        return pageSize;
    }

    public int getCurrentPage() {
        return currentPage;
    }
}
