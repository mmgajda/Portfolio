<?php
require(__DIR__ . "/../../partials/nav.php");
/*UCID: mg936
**Date: 30 July 2023
**Comment: Logic to display, sort, and filter products in shop*/
$results = [];
$db = getDB();
//process filters/sorting
//Sort and Filters
$col = se($_GET, "col", "unit_price", false);
//allowed list
if (!in_array($col, ["unit_price", "stock", "name", "category"])) {
    $col = "unit_price"; //default value, prevent sql injection
}
$order = se($_GET, "order", "asc", false);
//allowed list
if (!in_array($order, ["asc", "desc"])) {
    $order = "asc"; //default value, prevent sql injection
}
//get name partial search
$name = se($_GET, "name", "", false);

//dynamic query
$query = "SELECT id, name, description, unit_price, stock, category FROM Products items WHERE visibility = 1 and stock > 0"; //1=1 shortcut to conditionally build AND clauses
$params = []; //define default params, add keys as needed and pass to execute
//apply name filter
if (!empty($name)) {
    $query .= " AND name like :name";
    $params[":name"] = "%$name%";
}
//apply column and order sort
if (!empty($col) && !empty($order)) {
    $query .= " ORDER BY $col $order"; //be sure you trust these values, I validate via the in_array checks above
}
$stmt = $db->prepare($query); //dynamically generated query
try {
    $stmt->execute($params); //dynamically populated params to bind
    $r = $stmt->fetchAll(PDO::FETCH_ASSOC);
    if ($r) {
        $results = $r;
    }
} catch (PDOException $e) {
    error_log(var_export($e, true));
    flash("Error fetching items", "danger");
}
?>
<script>
    function purchase(item) {
        console.log("TODO purchase item", item);
        //alert("It's almost like you purchased an item, but not really");
        if (add_to_cart) {
            add_to_cart(item);
        }
    }
</script>

<div class="container-fluid">
    <h1>Shop</h1>
    <!--UCID: mg936
        Date: 30 July 2023
        Comment: Forms and logic to sort products in shop-->
    <form class="row row-cols-auto g-3 align-items-center">
        <div class="col">
            <div class="input-group" data="i">
                <div class="input-group-text">Name</div>
                <input class="form-control" name="name" value="<?php se($name); ?>" />
            </div>
        </div>
        <div class="col">
            <div class="input-group">
                <div class="input-group-text">Sort</div>
                <!-- make sure these match the in_array filter above-->
                <select class="form-control bg-info" name="col" value="<?php se($col); ?>" data="took">
                    <option value="unit_price">Cost</option>
                    <option value="stock">Stock</option>
                    <option value="name">Name</option>
                    <option value="category">Category</option>
                </select>
                <script>
                    //quick fix to ensure proper value is selected since
                    //value setting only works after the options are defined and php has the value set prior
                    document.forms[0].col.value = "<?php se($col); ?>";
                </script>
                <select class="form-control" name="order" value="<?php se($order); ?>">
                    <option class="bg-white" value="asc">Up</option>
                    <option class="bg-white" value="desc">Down</option>
                </select>
                <script data="this">
                    //quick fix to ensure proper value is selected since
                    //value setting only works after the options are defined and php has the value set prior
                    document.forms[0].order.value = "<?php se($order); ?>";
                    if (document.forms[0].order.value === "asc") {
                        document.forms[0].order.className = "form-control bg-success";
                    } else {
                        document.forms[0].order.className = "form-control bg-danger";
                    }
                </script>
            </div>
        </div>
        <div class="col">
            <div class="input-group">
                <input type="submit" class="btn btn-primary" value="Apply" />
            </div>
        </div>
    </form>
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
                                <a href="product_details.php?id=<?php se($item, "id"); ?>" class="btn btn-primary">Product Details</a>
                                <?php if (has_role("Owner") || has_role("Admin")) : ?>
                                    <br>
                                    <br>
                                    <a class="btn btn-primary" href="admin/edit_products.php?id=<?php se($item, "id"); ?>">Edit</a>
                                <?php endif; ?>
                            </div>

                            <div class="card-footer">
                                Cost: <?php se($item, "unit_price"); ?>
                                <br>
                                <button onclick="purchase('<?php se($item, 'id'); ?>')" class="btn btn-primary">Add to Cart</button>
                            </div>
                        </div>
                    </div>
                <?php endforeach; ?>
            </div>
        </div>
        <div class="col-4" style="min-width:30em">
            <?php require(__DIR__ . "/../../partials/shopping_cart.php"); ?>
        </div>
    </div>
</div>

<?php
require(__DIR__ . "/../../partials/footer.php");
?>