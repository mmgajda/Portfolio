<?php
require(__DIR__ . "/../../../partials/nav.php");
if (!has_role("Owner") && !has_role("Admin")) {
    flash("You don't have permission to view this page", "warning");
    //die(header("Location: $BASE_PATH/home.php"));
    redirect("home.php");
}

$results = [];
$db = getDB();
$stmt = $db->prepare("SELECT id, name, description, unit_price, stock, visibility FROM Products WHERE stock > 0 LIMIT 50");
try {
    $stmt->execute();
    $r = $stmt->fetchAll(PDO::FETCH_ASSOC);
    if ($r) {
        $results = $r;
    }
} catch (PDOException $e) {
    error_log(var_export($e, true));
    flash("Error fetching items", "danger");
}
?>

<div class="container-fluid">
    <h1>Shop</h1>
    <div class="row">
        <div class="col">
            <div class="row row-cols-1 row-cols-sm-1 row-cols-md-2 row-cols-lg-3 g-4">
                <?php foreach ($results as $item) : ?>
                    <div class="col">
                        <div class="card bg-light" style="height:25em">
                            <div class="card-header">
                                Fresh is Best
                            </div>
                            <?php if (se($item, "image", "", false)) : ?>
                                <img src="<?php se($item, "image"); ?>" class="card-img-top" alt="...">
                            <?php endif; ?>

                            <div class="card-body">
                                <h5 class="card-title">Name: <?php se($item, "name"); ?></h5>
                                <p class="card-text">Description: <?php se($item, "description"); ?></p>
                            </div>
                            <div class="card-footer">
                                Cost: <?php se($item, "unit_price"); ?>
                                <br>
                                Visibility: <?php (se($item, "visibility"))?>
                                <br>
                                <a class="btn btn-primary btn-lg active" href="edit_products.php?id=<?php se($item, "id"); ?>">Edit</a>
                            </div>
                        </div>
                    </div>
                <?php endforeach; ?>
            </div>
        </div>
    </div>
</div>

<?php
require(__DIR__ . "/../../../partials/footer.php");
?>