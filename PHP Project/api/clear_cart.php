<?php
require_once(__DIR__ . "/../../../lib/functions.php");

if (session_status() != PHP_SESSION_ACTIVE) {
    session_start();
}

$response = ["status" => 400, "message" => "Invalid data"];
http_response_code(400);

if (is_logged_in()) {
    $db = getDB();
    $user_id = get_user_id();
    $stmt = $db->prepare("DELETE FROM Shopping_Cart WHERE user_id = :user_id");
    $stmt->bindValue(":user_id", $user_id, PDO::PARAM_INT);
    try {
        $stmt->execute();
        $response["status"] = 200;
        $response["message"] = "Cart cleared successfully";
        http_response_code(200);
    } catch (PDOException $e) {
        error_log("Clear cart error: " . var_export($e, true));
        $response["message"] = "Error clearing cart";
    }
} else {
    http_response_code(403);
    $response["status"] = 403;
    $response["message"] = "Must be logged in to clear the cart";
}

echo json_encode($response);
?>