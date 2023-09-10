<?php
require_once(__DIR__ . "/../../../lib/functions.php");
error_log("add_to_cart received data: " . var_export($_REQUEST, true));
if (session_status() != PHP_SESSION_ACTIVE) {
    session_start();
}
//handle the potentially incoming post request
$item_id = (int)se($_POST, "item_id", null, false);
$desired_quantity = (int)se($_POST, "desired_quantity", 0, false);
$response = ["status" => 400, "message" => "Invalid data"];
http_response_code(400);
if (isset($item_id)) {
    if (is_logged_in()) {
        $db = getDB();
        //note adding to cart doesn't verify price or quantity
        // Update the quantity in the database with the desired quantity
        $stmt = $db->prepare("UPDATE Shopping_Cart SET quantity = :q WHERE item_id = :item_id");
        $stmt->bindValue(":item_id", $item_id, PDO::PARAM_INT);
        $stmt->bindValue(":q", $desired_quantity, PDO::PARAM_INT);

        try {
            $stmt->execute();
            $response["status"] = 200;
            $response["message"] = "Cart quantity updated";
            http_response_code(200);
        } catch (PDOException $e) {
            error_log("Update cart quantity error: " . var_export($e, true));
            $response["message"] = "Error updating cart quantity";
        }
    } else {
        http_response_code(403);
        $response["status"] = 403;
        $response["message"] = "Must be logged in to update cart quantity";
    }
}
echo json_encode($response);
?>