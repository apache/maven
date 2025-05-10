/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.impl.model.profile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

/**
 * The {@code ConditionParser} class is responsible for parsing and evaluating expressions.
 * It supports tokenizing the input expression and resolving custom functions passed in a map.
 * This class implements a recursive descent parser to handle various operations including
 * arithmetic, logical, and comparison operations, as well as function calls.
 */
public class ConditionParser {

    /**
     * A functional interface that represents an expression function to be applied
     * to a list of arguments. Implementers can define custom functions.
     */
    public interface ExpressionFunction {
        /**
         * Applies the function to the given list of arguments.
         *
         * @param args the list of arguments passed to the function
         * @return the result of applying the function
         */
        Object apply(List<Object> args);
    }

    private final Map<String, ExpressionFunction> functions; // Map to store functions by their names
    private final UnaryOperator<String> propertyResolver; // Property resolver
    private List<String> tokens; // List of tokens derived from the expression
    private int current; // Keeps track of the current token index

    /**
     * Constructs a new {@code ConditionParser} with the given function mappings.
     *
     * @param functions a map of function names to their corresponding {@code ExpressionFunction} implementations
     * @param propertyResolver the property resolver
     */
    public ConditionParser(Map<String, ExpressionFunction> functions, UnaryOperator<String> propertyResolver) {
        this.functions = functions;
        this.propertyResolver = propertyResolver;
    }

    /**
     * Parses the given expression and returns the result of the evaluation.
     *
     * @param expression the expression to be parsed
     * @return the result of parsing and evaluating the expression
     */
    public Object parse(String expression) {
        this.tokens = tokenize(expression);
        this.current = 0;
        return parseExpression();
    }

    /**
     * Tokenizes the input expression into a list of string tokens for further parsing.
     * This method handles quoted strings, property aliases, and various operators.
     *
     * @param expression the expression to tokenize
     * @return a list of tokens
     */
    private List<String> tokenize(String expression) {
        List<String> tokens = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        char quoteType = 0;
        boolean inPropertyReference = false;

        for (int i = 0; i < expression.length(); i++) {
            char c = expression.charAt(i);

            if (quoteType != 0) {
                if (c == quoteType) {
                    quoteType = 0;
                    sb.append(c);
                    tokens.add(sb.toString());
                    sb.setLength(0);
                } else {
                    sb.append(c);
                }
                continue;
            }

            if (inPropertyReference) {
                if (c == '}') {
                    inPropertyReference = false;
                    tokens.add("${" + sb + "}");
                    sb.setLength(0);
                } else {
                    sb.append(c);
                }
                continue;
            }

            if (c == '$' && i + 1 < expression.length() && expression.charAt(i + 1) == '{') {
                if (!sb.isEmpty()) {
                    tokens.add(sb.toString());
                    sb.setLength(0);
                }
                inPropertyReference = true;
                i++; // Skip the '{'
                continue;
            }

            if (c == '"' || c == '\'') {
                if (!sb.isEmpty()) {
                    tokens.add(sb.toString());
                    sb.setLength(0);
                }
                quoteType = c;
                sb.append(c);
            } else if (c == ' ' || c == '(' || c == ')' || c == ',' || c == '+' || c == '>' || c == '<' || c == '='
                    || c == '!') {
                if (!sb.isEmpty()) {
                    tokens.add(sb.toString());
                    sb.setLength(0);
                }
                if (c != ' ') {
                    if ((c == '>' || c == '<' || c == '=' || c == '!')
                            && i + 1 < expression.length()
                            && expression.charAt(i + 1) == '=') {
                        tokens.add(c + "=");
                        i++; // Skip the next character
                    } else {
                        tokens.add(String.valueOf(c));
                    }
                }
            } else {
                sb.append(c);
            }
        }

        if (inPropertyReference) {
            throw new RuntimeException("Unclosed property reference: ${");
        }

        if (!sb.isEmpty()) {
            tokens.add(sb.toString());
        }

        return tokens;
    }

    /**
     * Parses the next expression from the list of tokens.
     *
     * @return the parsed expression as an object
     * @throws RuntimeException if there are unexpected tokens after the end of the expression
     */
    private Object parseExpression() {
        Object result = parseLogicalOr();
        if (current < tokens.size()) {
            throw new RuntimeException("Unexpected tokens after end of expression");
        }
        return result;
    }

    /**
     * Parses logical OR operations.
     *
     * @return the result of parsing logical OR operations
     */
    private Object parseLogicalOr() {
        Object left = parseLogicalAnd();
        while (current < tokens.size() && tokens.get(current).equals("||")) {
            current++;
            Object right = parseLogicalAnd();
            left = (boolean) left || (boolean) right;
        }
        return left;
    }

    /**
     * Parses logical AND operations.
     *
     * @return the result of parsing logical AND operations
     */
    private Object parseLogicalAnd() {
        Object left = parseComparison();
        while (current < tokens.size() && tokens.get(current).equals("&&")) {
            current++;
            Object right = parseComparison();
            left = (boolean) left && (boolean) right;
        }
        return left;
    }

    /**
     * Parses comparison operations.
     *
     * @return the result of parsing comparison operations
     */
    private Object parseComparison() {
        Object left = parseAddSubtract();
        while (current < tokens.size()
                && (tokens.get(current).equals(">")
                        || tokens.get(current).equals("<")
                        || tokens.get(current).equals(">=")
                        || tokens.get(current).equals("<=")
                        || tokens.get(current).equals("==")
                        || tokens.get(current).equals("!="))) {
            String operator = tokens.get(current);
            current++;
            Object right = parseAddSubtract();
            left = compare(left, operator, right);
        }
        return left;
    }

    /**
     * Parses addition and subtraction operations.
     *
     * @return the result of parsing addition and subtraction operations
     */
    private Object parseAddSubtract() {
        Object left = parseMultiplyDivide();
        while (current < tokens.size()
                && (tokens.get(current).equals("+") || tokens.get(current).equals("-"))) {
            String operator = tokens.get(current);
            current++;
            Object right = parseMultiplyDivide();
            if (operator.equals("+")) {
                left = add(left, right);
            } else {
                left = subtract(left, right);
            }
        }
        return left;
    }

    /**
     * Parses multiplication and division operations.
     *
     * @return the result of parsing multiplication and division operations
     */
    private Object parseMultiplyDivide() {
        Object left = parseUnary();
        while (current < tokens.size()
                && (tokens.get(current).equals("*") || tokens.get(current).equals("/"))) {
            String operator = tokens.get(current);
            current++;
            Object right = parseUnary();
            if (operator.equals("*")) {
                left = multiply(left, right);
            } else {
                left = divide(left, right);
            }
        }
        return left;
    }

    /**
     * Parses unary operations (negation).
     *
     * @return the result of parsing unary operations
     */
    private Object parseUnary() {
        if (current < tokens.size() && tokens.get(current).equals("-")) {
            current++;
            Object value = parseUnary();
            return negate(value);
        }
        return parseTerm();
    }

    /**
     * Parses individual terms (numbers, strings, booleans, parentheses, functions).
     *
     * @return the parsed term
     * @throws RuntimeException if the expression ends unexpectedly or contains unknown tokens
     */
    private Object parseTerm() {
        if (current >= tokens.size()) {
            throw new RuntimeException("Unexpected end of expression");
        }

        String token = tokens.get(current);
        if (token.equals("(")) {
            return parseParentheses();
        } else if (functions.containsKey(token)) {
            return parseFunction();
        } else if ((token.startsWith("\"") && token.endsWith("\"")) || (token.startsWith("'") && token.endsWith("'"))) {
            current++;
            return token.length() > 1 ? token.substring(1, token.length() - 1) : "";
        } else if (token.equalsIgnoreCase("true") || token.equalsIgnoreCase("false")) {
            current++;
            return Boolean.parseBoolean(token);
        } else if (token.startsWith("${") && token.endsWith("}")) {
            current++;
            String propertyName = token.substring(2, token.length() - 1);
            return propertyResolver.apply(propertyName);
        } else {
            try {
                current++;
                return Double.parseDouble(token);
            } catch (NumberFormatException e) {
                // If it's not a number, treat it as a variable or unknown function
                return parseVariableOrUnknownFunction();
            }
        }
    }

    /**
     * Parses a token that could be either a variable or an unknown function.
     *
     * @return the result of parsing a variable or unknown function
     * @throws RuntimeException if an unknown function is encountered
     */
    private Object parseVariableOrUnknownFunction() {
        current--; // Move back to the token we couldn't parse as a number
        String name = tokens.get(current);
        current++;

        // Check if it's followed by an opening parenthesis, indicating a function call
        if (current < tokens.size() && tokens.get(current).equals("(")) {
            // It's a function call, parse it as such
            List<Object> args = parseArgumentList();
            if (functions.containsKey(name)) {
                return functions.get(name).apply(args);
            } else {
                throw new RuntimeException("Unknown function: " + name);
            }
        } else {
            // It's a variable
            // Here you might want to handle variables differently
            // For now, we'll throw an exception
            throw new RuntimeException("Unknown variable: " + name);
        }
    }

    /**
     * Parses a list of arguments for a function call.
     *
     * @return a list of parsed arguments
     * @throws RuntimeException if there's a mismatch in parentheses
     */
    private List<Object> parseArgumentList() {
        List<Object> args = new ArrayList<>();
        current++; // Skip the opening parenthesis
        while (current < tokens.size() && !tokens.get(current).equals(")")) {
            args.add(parseLogicalOr());
            if (current < tokens.size() && tokens.get(current).equals(",")) {
                current++;
            }
        }
        if (current >= tokens.size() || !tokens.get(current).equals(")")) {
            throw new RuntimeException("Mismatched parentheses: missing closing parenthesis in function call");
        }
        current++; // Skip the closing parenthesis
        return args;
    }

    /**
     * Parses a function call.
     *
     * @return the result of the function call
     */
    private Object parseFunction() {
        String functionName = tokens.get(current);
        current++;
        List<Object> args = parseArgumentList();
        return functions.get(functionName).apply(args);
    }

    /**
     * Parses an expression within parentheses.
     *
     * @return the result of parsing the expression within parentheses
     * @throws RuntimeException if there's a mismatch in parentheses
     */
    private Object parseParentheses() {
        current++; // Skip the opening parenthesis
        Object result = parseLogicalOr();
        if (current >= tokens.size() || !tokens.get(current).equals(")")) {
            throw new RuntimeException("Mismatched parentheses: missing closing parenthesis");
        }
        current++; // Skip the closing parenthesis
        return result;
    }

    /**
     * Adds two objects, handling string concatenation and numeric addition.
     *
     * @param left the left operand
     * @param right the right operand
     * @return the result of the addition
     * @throws RuntimeException if the operands cannot be added
     */
    private static Object add(Object left, Object right) {
        if (left instanceof String || right instanceof String) {
            return toString(left) + toString(right);
        } else if (left instanceof Number leftNumber && right instanceof Number rightNumber) {
            return leftNumber.doubleValue() + rightNumber.doubleValue();
        } else {
            throw new RuntimeException("Cannot add " + left + " and " + right);
        }
    }

    /**
     * Negates a numeric value.
     *
     * @param value the value to negate
     * @return the negated value
     * @throws RuntimeException if the value cannot be negated
     */
    private Object negate(Object value) {
        if (value instanceof Number number) {
            return -number.doubleValue();
        }
        throw new RuntimeException("Cannot negate non-numeric value: " + value);
    }

    /**
     * Subtracts the right operand from the left operand.
     *
     * @param left the left operand
     * @param right the right operand
     * @return the result of the subtraction
     * @throws RuntimeException if the operands cannot be subtracted
     */
    private static Object subtract(Object left, Object right) {
        if (left instanceof Number leftNumber && right instanceof Number rightNumber) {
            return leftNumber.doubleValue() - rightNumber.doubleValue();
        } else {
            throw new RuntimeException("Cannot subtract " + right + " from " + left);
        }
    }

    /**
     * Multiplies two numeric operands.
     *
     * @param left the left operand
     * @param right the right operand
     * @return the result of the multiplication
     * @throws RuntimeException if the operands cannot be multiplied
     */
    private static Object multiply(Object left, Object right) {
        if (left instanceof Number leftNumber && right instanceof Number rightNumber) {
            return leftNumber.doubleValue() * rightNumber.doubleValue();
        } else {
            throw new RuntimeException("Cannot multiply " + left + " and " + right);
        }
    }

    /**
     * Divides the left operand by the right operand.
     *
     * @param left the left operand (dividend)
     * @param right the right operand (divisor)
     * @return the result of the division
     * @throws RuntimeException if the operands cannot be divided
     * @throws ArithmeticException if attempting to divide by zero
     */
    private static Object divide(Object left, Object right) {
        if (left instanceof Number leftNumber && right instanceof Number rightNumber) {
            double divisor = rightNumber.doubleValue();
            if (divisor == 0) {
                throw new ArithmeticException("Division by zero");
            }
            return leftNumber.doubleValue() / divisor;
        } else {
            throw new RuntimeException("Cannot divide " + left + " by " + right);
        }
    }

    /**
     * Compares two objects based on the given operator.
     * Supports comparison of numbers and strings, and equality checks for null values.
     *
     * @param left the left operand
     * @param operator the comparison operator (">", "<", ">=", "<=", "==", or "!=")
     * @param right the right operand
     * @return the result of the comparison (a boolean value)
     * @throws IllegalStateException if an unknown operator is provided
     * @throws RuntimeException if the operands cannot be compared
     */
    private static Object compare(Object left, String operator, Object right) {
        if (left == null && right == null) {
            return true;
        }
        if (left == null || right == null) {
            if ("==".equals(operator)) {
                return false;
            } else if ("!=".equals(operator)) {
                return true;
            }
        }
        if (left instanceof Number leftNumber && right instanceof Number rightNumber) {
            double leftVal = leftNumber.doubleValue();
            double rightVal = rightNumber.doubleValue();
            return switch (operator) {
                case ">" -> leftVal > rightVal;
                case "<" -> leftVal < rightVal;
                case ">=" -> leftVal >= rightVal;
                case "<=" -> leftVal <= rightVal;
                case "==" -> Math.abs(leftVal - rightVal) < 1e-9;
                case "!=" -> Math.abs(leftVal - rightVal) >= 1e-9;
                default -> throw new IllegalStateException("Unknown operator: " + operator);
            };
        } else if (left instanceof String leftString && right instanceof String rightString) {
            int comparison = leftString.compareTo(rightString);
            return switch (operator) {
                case ">" -> comparison > 0;
                case "<" -> comparison < 0;
                case ">=" -> comparison >= 0;
                case "<=" -> comparison <= 0;
                case "==" -> comparison == 0;
                case "!=" -> comparison != 0;
                default -> throw new IllegalStateException("Unknown operator: " + operator);
            };
        }
        throw new RuntimeException("Cannot compare " + left + " and " + right + " with operator " + operator);
    }

    /**
     * Converts an object to a string representation.
     * If the object is a {@code Double}, it formats it without any decimal places.
     * Otherwise, it uses the {@code String.valueOf} method.
     *
     * @param value the object to convert to a string
     * @return the string representation of the object
     */
    public static String toString(Object value) {
        if (value instanceof Double || value instanceof Float) {
            double doubleValue = ((Number) value).doubleValue();
            if (doubleValue == Math.floor(doubleValue) && !Double.isInfinite(doubleValue)) {
                return "%.0f".formatted(doubleValue);
            }
        }
        return String.valueOf(value);
    }

    /**
     * Converts an object to a boolean value.
     * If the object is:
     * - a {@code Boolean}, returns its value directly.
     * - a {@code String}, returns {@code true} if the string is non-blank.
     * - a {@code Number}, returns {@code true} if its integer value is not zero.
     * For other object types, returns {@code true} if the object is non-null.
     *
     * @param value the object to convert to a boolean
     * @return the boolean representation of the object
     */
    public static Boolean toBoolean(Object value) {
        if (value instanceof Boolean b) {
            return b; // Returns the boolean value
        } else if (value instanceof String s) {
            return !s.isBlank(); // True if the string is not blank
        } else if (value instanceof Number b) {
            return b.intValue() != 0; // True if the number is not zero
        } else {
            return value != null; // True if the object is not null
        }
    }

    /**
     * Converts an object to a double value.
     * If the object is:
     * - a {@code Number}, returns its double value.
     * - a {@code String}, tries to parse it as a double.
     * - a {@code Boolean}, returns {@code 1.0} for {@code true}, {@code 0.0} for {@code false}.
     * If the object cannot be converted, a {@code RuntimeException} is thrown.
     *
     * @param value the object to convert to a double
     * @return the double representation of the object
     * @throws RuntimeException if the object cannot be converted to a double
     */
    public static double toDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue(); // Converts number to double
        } else if (value instanceof String string) {
            try {
                return Double.parseDouble(string); // Tries to parse string as double
            } catch (NumberFormatException e) {
                throw new RuntimeException("Cannot convert string to number: " + value);
            }
        } else if (value instanceof Boolean bool) {
            return bool ? 1.0 : 0.0; // True = 1.0, False = 0.0
        } else {
            throw new RuntimeException("Cannot convert to number: " + value);
        }
    }

    /**
     * Converts an object to an integer value.
     * If the object is:
     * - a {@code Number}, returns its integer value.
     * - a {@code String}, tries to parse it as an integer, or as a double then converted to an integer.
     * - a {@code Boolean}, returns {@code 1} for {@code true}, {@code 0} for {@code false}.
     * If the object cannot be converted, a {@code RuntimeException} is thrown.
     *
     * @param value the object to convert to an integer
     * @return the integer representation of the object
     * @throws RuntimeException if the object cannot be converted to an integer
     */
    public static int toInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue(); // Converts number to int
        } else if (value instanceof String string) {
            try {
                return Integer.parseInt(string); // Tries to parse string as int
            } catch (NumberFormatException e) {
                // If string is not an int, tries parsing as double and converting to int
                try {
                    return (int) Double.parseDouble((String) value);
                } catch (NumberFormatException e2) {
                    throw new RuntimeException("Cannot convert string to integer: " + value);
                }
            }
        } else if (value instanceof Boolean bool) {
            return bool ? 1 : 0; // True = 1, False = 0
        } else {
            throw new RuntimeException("Cannot convert to integer: " + value);
        }
    }
}
