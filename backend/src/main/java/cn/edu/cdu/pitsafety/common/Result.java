package cn.edu.cdu.pitsafety.common;

import lombok.Data;

/**
 * 后端统一返回结果封装类
 * 规范了前后端交互的数据格式
 */
@Data
public class Result<T> {

    private Integer code; // 状态码：200代表成功，其他代表失败
    private String msg;   // 提示信息
    private T data;       // 实际返回的数据内容

    // 1. 无数据的成功返回
    public static <T> Result<T> success() {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMsg("操作成功");
        return result;
    }

    // 2. 带数据的成功返回
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMsg("操作成功");
        result.setData(data);
        return result;
    }

    // 3. 失败返回
    public static <T> Result<T> error(Integer code, String msg) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMsg(msg);
        return result;
    }
}