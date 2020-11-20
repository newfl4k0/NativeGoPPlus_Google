package com.pplus.go.Utils;

import java.util.regex.Pattern;

public final class RegexValidator {
    private static String emailRegex         = "^(([^<>()\\[\\]\\\\.,;:\\s@\"]+(\\.[^<>()\\[\\]\\\\.,;:\\s@\"]+)*)|(\".+\"))@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}])|(([a-zA-Z\\-0-9]+\\.)+[a-zA-Z]{2,}))$";
    private static String nameRegex          = "^[a-zA-Z\\u00C0-\\u017F ]+$";
    private static String alphaRegex         = "^[a-z]+$";
    private static String alphaNumericRegex  = "^[a-z0-9]+$";
    private static String alphaDashRegex     = "^[A-z0-9@*_\\-]+$";
    private static String passwordRegex      = ".*[\\u00E0-\\u00FC<>`¨´~].*";
    private static String uppercaseRegex     = ".*[A-Z].*";
    private static String lowercaseRegex     = ".*[a-z].*";
    private static String numberRegex        = ".*[0-9].*";
    private static String naturalRegex       = "^[0-9]+$";
    private static String naturalNoZeroRegex = "^[1-9][0-9]*$";
    private static String numericDashRegex   = ".*[\\d\\-\\s].*";
    private static String specialRegex       = ".*[-[\\]{}()*+¿?¡!.,\\^$|#\\_\\/='&%$·@|:]].*";
    private static String urlRegex           = "^((http|https):\\/\\/(\\w+:{0,1}\\w*@)?(\\S+)|)(:[0-9]+)?(\\/|\\/([\\w#!:.?+=&%@!\\-\\/]))?$";
    private static String textRegex          = "^([A-zÀ-ü0-9 .,:?!¿¡*#])*$";
    private static String zipCodeRegex       = "^[0-9]{5}(-[0-9]{4})?$";
    private static String ruleRegex          = "^(.+?)\\[(.+)\\]$";
    private static String numericRegex       = "^[0-9]+$";
    private static String integerRegex       = "^\\-?[0-9]+$";
    private static String decimalRegex       = "^\\-?[0-9]*\\.?[0-9]+$";
    private static String monthRegex         = "^1[0-2]$|^0[1-9]$";
    private static String cvvRegex           = "^[0-9]{3,4}$";

    public static String message_required           = "El campo %s es requerido.";
    public static String message_matches            = "El campo %s es no es igual al campo %p.";
    public static String message_valid_email        = "El campo %s no es una dirección de correo.";
    public static String message_valid_emails       = "El campo %s no contiene direcciones de correo válidas.";
    public static String message_min_length         = "El campo %s debe tener al menos %p caracteres de longitud.";
    public static String message_max_length         = "El campo %s no debe exceder de %p caracteres de longitud.";
    public static String message_exact_length       = "El campo %s debe tener exactamente %p caracteres de longitud.";
    public static String message_greater_than       = "El campo %s debe ser mayor a %p.";
    public static String message_is_checked         = "%s debe ser marcado.";
    public static String message_agree_on           = "Acepta %s.";
    public static String message_age_at_least       = "Debes de tener al menos %p años para continuar";
    public static String message_less_than          = "El campo %s debe ser menor a %p.";
    public static String message_alpha              = "El campo %s solo puede contener letras.";
    public static String message_alpha_numeric      = "El campo %s solo puede contener caracteres alfanuméricos.";
    public static String message_alpha_dash         = "El campo %s solo puede contener caracteres alfanuméricos y los siguientes caracteres especiales @ * _ -";
    public static String message_numeric            = "El campo %s solo puede contener números.";
    public static String message_integer            = "El campo %s debe ser entero.";
    public static String message_decimal            = "El campo %s debe ser decimal.";
    public static String message_is_natural         = "El campo %s solo puede contener números positivos";
    public static String message_is_natural_no_zero = "El campo %s debe ser mayor a cero.";
    public static String message_valid_ip           = "El campo %s debe ser una IP válida";
    public static String message_valid_base64       = "El campo %s debe contener un valor base64";
    public static String message_valid_credit_card  = "El campo %s debe ser un número de tarjeta válido";
    public static String message_valid_url          = "El campo %s debe ser una URL válida";
    public static String message_valid_zip_code     = "El campo %s debe contener un código postal válido.";
    public static String message_name               = "El campo %s no es un nombre válido.";
    public static String message_valid_password     = "El campo %s no es una contraseña válida. Mínimo 8 caracteres, máximo 16. Debe contener mínimo: una letra mayúscula, una minúscula, un caracter especial y números. No debe contener acentos ni los siguientes caracteres <>";
    public static String message_valid_confirm      = "El campo %s no coincide con el campo Contraseña.";
    public static String message_valid_text         = "El campo %s no es un texto válido.";
    public static String message_valid_month        = "El campo %s no es un mes válido.";
    public static String message_valid_cvv          = "El valor %s no es un código de seguridad válido.";

    public static boolean isEmail(String value){
        return Pattern.matches(emailRegex, value);
    }

    public static boolean isName(String value){
        return Pattern.matches(nameRegex, value);
    }

    public static boolean isAlpha(String value){
        return Pattern.matches(alphaRegex, value);
    }

    public static boolean isAlphaNumeric(String value){
        return Pattern.matches(alphaNumericRegex, value);
    }

    public static boolean isAlphaDash(String value){
        return Pattern.matches(alphaDashRegex, value);
    }

    public static boolean isPassword(String value){
        boolean invalid_match = !Pattern.matches(passwordRegex, value);
        boolean hasUpperCase  = isUpperCase(value);
        boolean hasLowerCase  = isLowerCase(value);
        boolean hasNumber     = isNumber(value);
        boolean hasSpecial    = Pattern.matches(specialRegex, value);
        boolean valid_length  = value.length() >= 8 && value.length() <=16;

        return invalid_match && hasUpperCase && hasLowerCase && hasNumber && hasSpecial && valid_length;
    }

    public static boolean isUpperCase(String value){
        return Pattern.matches(uppercaseRegex, value);
    }

    public static boolean isLowerCase(String value){
        return Pattern.matches(lowercaseRegex, value);
    }

    public static boolean isNumber(String value){
        return Pattern.matches(numberRegex, value);
    }

    public static boolean isNatural(String value){
        return Pattern.matches(naturalRegex, value);
    }

    public static boolean isNaturalNoZero(String value){
        return Pattern.matches(naturalNoZeroRegex, value);
    }


    public static boolean isNumericDash(String value){
        return Pattern.matches(numericDashRegex, value);
    }

    public static boolean isURL(String value){
        return Pattern.matches(urlRegex, value);
    }

    public static boolean isText(String value){
        return Pattern.matches(textRegex, value);
    }

    public static boolean isZip(String value){
        return Pattern.matches(zipCodeRegex, value);
    }

    public static boolean isRule(String value){
        return Pattern.matches(ruleRegex, value);
    }

    public static boolean isNumeric(String value){
        return Pattern.matches(numericRegex, value);
    }

    public static boolean isInteger(String value){
        return Pattern.matches(integerRegex, value);
    }

    public static boolean isDecimal(String value){
        return Pattern.matches(decimalRegex, value);
    }

    public static boolean validateRequired(String value){
        return value.isEmpty() == false && value != null;
    }

    public static boolean isMonth(String value) {
        return Pattern.matches(monthRegex, value);
    }

    public static boolean isCVV(String value) {
        return Pattern.matches(cvvRegex, value);
    }

    public static boolean matches(String value1, String value2) {
        return value1 == value2;
    }

    public static String replaceMessage(String message, String value){
        String[] values = value.split(",");

        String name = values[0];
        String val  = "";

        if (values.length > 1) {
            val = values[1];
        }

        return message.replace("%s", name).replace("%p", val);
    }

}