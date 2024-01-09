package actions;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDate;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import constants.AttributeConst;
import constants.ForwardConst;
import constants.PropertyConst;

/**
 * 各Actionクラスの親クラス。共通処理を行う。
 * すべてのActionクラスのスーパークラスとなるクラス
 */
public abstract class ActionBase {
    protected ServletContext context; //Webアプリケーションのコンテキスト情報
    protected HttpServletRequest request; //リクエスト情報のオブジェクト
    protected HttpServletResponse response; //レスポンス情報のオブジェクト

    /**
     * 初期化処理
     * 各クラスフィールドの値を設定します。
     * サーブレットコンテキスト、リクエスト、レスポンスをクラスフィールドに設定
     * @param servletContext
     * @param servletRequest
     * @param servletResponse
     */
    public void init(
            ServletContext servletContext,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        this.context = servletContext;
        this.request = servletRequest;
        this.response = servletResponse;
    }

    /**
     * フロントコントローラから呼び出されるメソッド
     * 各サブクラスで内容を実装します。
     * @throws ServletException
     * @throws IOException
     */
    public abstract void process() throws ServletException, IOException;

    /**
     * パラメータのcommandの値に該当するメソッドを実行する
     * commandの値が不正の場合はエラー画面を呼び出します。
     * @throws ServletException
     * @throws IOException
     */
    protected void invoke()
            throws ServletException, IOException {

        Method commandMethod;
        try {

            //パラメータからcommandを取得
            String command = request.getParameter(ForwardConst.CMD.getValue());

            //commandに該当するメソッドを実行する
            //(例: action=Employee command=show の場合 EmployeeActionクラスのshow()メソッドを実行する)
            commandMethod = this.getClass().getDeclaredMethod(command, new Class[0]);
            commandMethod.invoke(this, new Object[0]); //メソッドに渡す引数はなし

        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException | NullPointerException e) {

            //発生した例外をコンソールに表示
            e.printStackTrace();
            //commandの値が不正で実行できない場合エラー画面を呼び出し
            forward(ForwardConst.FW_ERR_UNKNOWN);
        }

    }

    /**
     * 指定されたjspの呼び出しを行う
     * @param target 遷移先jsp画面のファイル名(拡張子を含まない)
     * @throws ServletException
     * @throws IOException
     */
    protected void forward(ForwardConst target) throws ServletException, IOException {

        //jspファイルの相対パスを作成
        String forward = String.format("/WEB-INF/views/%s.jsp", target.getValue());
        RequestDispatcher dispatcher = request.getRequestDispatcher(forward);

        //jspファイルの呼び出し
        dispatcher.forward(request, response);

    }

    /**
     * URLを構築しリダイレクトを行う
     * @param action パラメータに設定する値
     * @param command パラメータに設定する値
     * @throws ServletException
     * @throws IOException
     */
    protected void redirect(ForwardConst action, ForwardConst command)
            throws ServletException, IOException {

        //URLを構築
        String redirectUrl = request.getContextPath() + "/?action=" + action.getValue();
        if (command != null) {
            redirectUrl = redirectUrl + "&command=" + command.getValue();
        }

        //URLへリダイレクト
        response.sendRedirect(redirectUrl);

    }

    /**
     * CSRF対策 token不正の場合はエラー画面を表示
     * リクエストからパラメータtokenの値を取得し、セッションIDと比較します。
     * token不正の場合はエラー画面を表示します。
     * 画面の入力フォームからのリクエストについてはこのメソッドを利用して正当性を確認します。
     * @return true: token有効 false: token不正
     * @throws ServletException
     * @throws IOException
     */
    protected boolean checkToken() throws ServletException, IOException {

        //パラメータからtokenの値を取得
        String _token = getRequestParam(AttributeConst.TOKEN);

        if (_token == null || !(_token.equals(getTokenId()))) {

            //tokenが設定されていない、またはセッションIDと一致しない場合はエラー画面を表示
            forward(ForwardConst.FW_ERR_UNKNOWN);

            return false;
        } else {
            return true;
        }

    }

    /**
     * セッションIDを取得する
     * 取得した値は入力フォームの隠し項目として埋め込み、リクエストの正当性の判定に利用します。
     * @return セッションID
     */
    protected String getTokenId() {
        return request.getSession().getId();
    }

    /**
     * リクエストから表示を要求されているページ数を取得し、返却する
     * 一覧表示のページネーションで利用します。要求がない場合は1を返却します。
     * @return 要求されているページ数(要求がない場合は1)
     */
    protected int getPage() {
        int page;
        page = toNumber(request.getParameter(AttributeConst.PAGE.getValue()));
        if (page == Integer.MIN_VALUE) {
            page = 1;
        }
        return page;
    }

    /**
     * 文字列を数値に変換する
     * クエリパラメータはStringで取得するため、id等については数値に変換する必要があります。
     * @param strNumber 変換前文字列
     * @return 変換後数値
     */
    protected int toNumber(String strNumber) {
        int number = 0;
        try {
            number = Integer.parseInt(strNumber);
        } catch (Exception e) {
            number = Integer.MIN_VALUE;
        }
        return number;
    }

    /**
     * 文字列をLocalDate型に変換する
     * @param strDate 変換前文字列
     * @return 変換後LocalDateインスタンス
     */
    protected LocalDate toLocalDate(String strDate) {
        if (strDate == null || strDate.equals("")) {
            return LocalDate.now();
        }
        return LocalDate.parse(strDate);
    }

    /**
     * リクエストパラメータから引数で指定したパラメータ名の値を返却する
     * @param key パラメータ名
     * @return パラメータの値
     */
    protected String getRequestParam(AttributeConst key) {
        return request.getParameter(key.getValue());
    }

    /**
     * リクエストスコープにパラメータを設定する
     * 第2引数の型Vはジェネリクス（Generics・総称型）というものです。
     * 与えた引数の型がこのVにあたる、という意味です。
     * すべての型を引数にとることができます。
     * @param key パラメータ名
     * @param value パラメータの値
     */
    protected <V> void putRequestScope(AttributeConst key, V value) {
        request.setAttribute(key.getValue(), value);
    }

    /**
     * セッションスコープから指定されたパラメータの値を取得し、返却する
     * セッションにはあらゆる型のオブジェクトを格納できます。
     * そのオブジェクトを取得するため、メソッドの戻り値の型もさまざまになります。
     * 戻り値をジェネリクスで記述しています。
     * @param key パラメータ名
     * @return パラメータの値
     */
    @SuppressWarnings("unchecked")
    protected <R> R getSessionScope(AttributeConst key) {
        return (R) request.getSession().getAttribute(key.getValue());
    }

    /**
     * セッションスコープにパラメータを設定する
     * @param key パラメータ名
     * @param value パラメータの値
     */
    protected <V> void putSessionScope(AttributeConst key, V value) {
        request.getSession().setAttribute(key.getValue(), value);
    }

    /**
     * セッションスコープから指定された名前のパラメータを除去する
     * @param key パラメータ名
     */
    protected void removeSessionScope(AttributeConst key) {
        request.getSession().removeAttribute(key.getValue());
    }

    /**
     * アプリケーションスコープから指定されたパラメータの値を取得し、返却する
     * @param key パラメータ名
     * @return パラメータの値
     */
    @SuppressWarnings("unchecked")
    protected <R> R getContextScope(PropertyConst key) {
        return (R) context.getAttribute(key.getValue());
    }

}
